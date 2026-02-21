(ns iam-clj-api.oauth.authorization
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [iam-clj-api.oauth.model :as model]
            [iam-clj-api.oauth.crypto :as crypto]
            [iam-clj-api.oauth.config :as config]
            [iam-clj-api.user.controller :as user-controller]
            [iam-clj-api.user.model :as user-model]
            [lib.response :refer [error work]]
            [ring.util.codec :as codec]))

(defn- now-plus-seconds [seconds]
  (java.sql.Timestamp. (+ (System/currentTimeMillis) (* seconds 1000))))

(defn- param [request key]
  (or (get-in request [:params key])
      (get-in request [:params (name key)])
      (get-in request [:query-params key])
      (get-in request [:query-params (name key)])))

(defn- split-scopes [scope-raw]
  (if (str/blank? scope-raw)
    []
    (->> (str/split (str scope-raw) #"\s+")
         (remove str/blank?)
         distinct
         vec)))

(defn- normalize-client [client]
  (when client
    (assoc client
           :redirect_uris (model/get-client-redirect-uris (:id client))
           :grants (set (model/get-client-grants (:id client)))
           :scopes (set (model/get-client-scopes (:id client))))))

(defn- validate-scopes [client requested-scopes]
  (let [normalize-scope (fn [scope]
                          (-> scope str str/trim str/lower-case))
        allowed (->> (:scopes client)
                     (map normalize-scope)
                     (remove str/blank?)
                     set)
        requested (->> requested-scopes
                       (map normalize-scope)
                       (remove str/blank?)
                       set)]
    (log/info "Validating requested scopes"
              {:client_id (:id client)
               :requested requested
               :allowed allowed})
    (or (empty? requested)
        (every? allowed requested))))

(defn- with-state [url state]
  (if (str/blank? state)
    url
    (str url "&state=" (codec/url-encode state))))

(defn- prompt-login? [prompt-raw]
  (->> (str/split (str (or prompt-raw "")) #"\s+")
       (map str/lower-case)
       (some #(= % "login"))
       boolean))

(defn- max-age-zero? [max-age-raw]
  (let [value (str/trim (str (or max-age-raw "")))]
    (or (= "0" value)
        (try
          (zero? (Long/parseLong value))
          (catch Exception _
            false)))))

(defn- force-reauth? [request]
  (or (prompt-login? (param request :prompt))
      (max-age-zero? (param request :max_age))))

(defn- strip-login-prompt [query-string]
  (let [params (codec/form-decode (or query-string ""))
        prompt-raw (get params "prompt")
        remaining-prompts (->> (str/split (str (or prompt-raw "")) #"\s+")
                               (map str/lower-case)
                               (remove str/blank?)
                               (remove #(= % "login"))
                               vec)
        cleaned-params (cond-> (dissoc params "prompt")
                         (seq remaining-prompts) (assoc "prompt" (str/join " " remaining-prompts)))]
    (codec/form-encode cleaned-params)))

(defn- oauth-login-redirect
  ([request]
   (oauth-login-redirect request false))
  ([request force-login?]
   (let [query-string (if force-login?
                        (strip-login-prompt (:query-string request))
                        (or (:query-string request) ""))]
     {:status 302
      :headers {"Location" (str config/frontend-oauth-login-url
                                "?query_string="
                                (codec/url-encode query-string))}})))

(defn authorize [request]
  (let [response-type (param request :response_type)
        client-id (param request :client_id)
        redirect-uri (param request :redirect_uri)
        state (param request :state)
        scope (param request :scope)
        scope-list (split-scopes scope)
        force-login (force-reauth? request)
        user-id (get-in request [:session :user-id])
        client (normalize-client (model/get-client-by-client-id client-id))]
    (cond
      (not= "code" response-type)
      (error 400 "unsupported_response_type")

      (nil? client)
      (error 400 "invalid_client")

      (not (contains? (:grants client) "authorization_code"))
      (error 400 "unauthorized_client")

      (not (contains? (set (:redirect_uris client)) redirect-uri))
      (error 400 "invalid_redirect_uri")

      (not (validate-scopes client scope-list))
      (do (log/warn "Client credentials grant with invalid scopes requested"
                    {:client_id (:id client) :requested scope-list})
          {:status 400 :body {:error "invalid_scope"}})

      (or (nil? user-id) force-login)
      (oauth-login-redirect request force-login)

      (boolean (:force_password_reset (user-model/get-user-by-id user-id)))
      (error 403 "Password reset required before access is allowed")

      :else
      (let [raw-code (crypto/random-token 32)
            code-hash (crypto/sha256 raw-code)]
        (model/create-auth-code {:code_hash code-hash
                                 :client_id (:id client)
                                 :user_id user-id
                                 :redirect_uri redirect-uri
                                 :scope (model/scope-string scope-list)
                                 :expires_at (now-plus-seconds config/auth-code-ttl-seconds)})
        {:status 302
         :headers {"Location" (with-state (str redirect-uri "?code=" (codec/url-encode raw-code)
                                               "&consent_tooltip=" (codec/url-encode "Consent is skipped for trusted clients"))
                                           state)}}))))

(defn oauth-login [request]
  (let [login-id (param request :login_id)
        password (param request :password)
        query-string (param request :query_string)
        login-response (user-controller/login-user login-id password)]
    (if (= 200 (:status login-response))
      (let [user (get-in login-response [:body :user])]
        {:status 302
         :session {:user-id (:id user)
                   :username (:username user)
                   :first-name (:first_name user)
                   :last-name (:last_name user)
                   :permissions (:permissions user)}
         :headers {"Location" (str "/oauth/authorize?" query-string)}})
              login-response)))

(defn oauth-login-context [request]
  (work 200 {:message "OAuth login required"
             :query-string (:query-string request)}))

(defn oauth-logout [request]
  (let [client-id (param request :client_id)
        state (param request :state)
        client (some-> client-id model/get-client-by-client-id normalize-client)
        redirect-uri (first (:redirect_uris client))
        login-location (if (str/blank? redirect-uri)
                         config/frontend-oauth-login-url
                         (let [uri (java.net.URI. redirect-uri)
                               base-path (or (.getPath uri) "")
                               app-prefix (str/replace base-path #"/oauth/callback$" "")]
                           (str (.getScheme uri) "://" (.getAuthority uri) app-prefix "/oauth/login")))
        location (if (str/blank? state)
                   login-location
                   (str login-location
                        (if (str/includes? login-location "?") "&" "?")
                        "state=" (codec/url-encode state)))]
    {:status 302
     :session nil
     :headers {"Location" location
               "Set-Cookie" (str config/session-cookie-name "=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax")}}))
