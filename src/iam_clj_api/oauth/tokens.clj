(ns iam-clj-api.oauth.tokens
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [iam-clj-api.oauth.model :as model]
            [iam-clj-api.oauth.crypto :as crypto]
            [iam-clj-api.oauth.config :as config]
            [iam-clj-api.user.model :as user-model]
            [lib.response :refer [success]])
  (:import (java.security KeyPairGenerator Signature)
           (java.security.interfaces RSAPublicKey RSAPrivateKey)
           (java.util Base64 UUID)))

(def ^:private oidc-key-id (str "oidc-" (UUID/randomUUID)))

(defonce ^:private oidc-keypair
  (delay
    (let [generator (KeyPairGenerator/getInstance "RSA")]
      (.initialize generator 2048)
      (.generateKeyPair generator))))

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

(defn- parse-basic-auth [request]
  (when-let [header (or (get-in request [:headers "authorization"])
                        (get-in request [:headers "Authorization"]))]
    (when (str/starts-with? (str/lower-case header) "basic ")
      (let [encoded (subs header 6)
            decoded (String. (.decode (java.util.Base64/getDecoder) encoded) "UTF-8")
            idx (.indexOf decoded ":")]
        (when (pos? idx)
          {:client_id (subs decoded 0 idx)
           :client_secret (subs decoded (inc idx))})))))

(defn- normalize-client [client]
  (when client
    (assoc client
           :redirect_uris (model/get-client-redirect-uris (:id client))
           :grants (set (model/get-client-grants (:id client)))
           :scopes (set (model/get-client-scopes (:id client))))))

(defn- authenticate-client [request]
  (let [basic (parse-basic-auth request)
        post-client-id (param request :client_id)
        post-client-secret (param request :client_secret)
        grant-type (param request :grant_type)
        client-id (or (:client_id basic) post-client-id)
        secret (or (:client_secret basic) post-client-secret)
        client (normalize-client (model/get-client-by-client-id client-id))]
    (cond
      (nil? client)
      {:error {:status 401 :body {:error "invalid_client"}}}

      (and (= "none" (:token_endpoint_auth_method client))
           (= "authorization_code" grant-type))
      {:client client}

      (= "client_secret_basic" (:token_endpoint_auth_method client))
      (if (and basic (crypto/check-secret secret (:client_secret_hash client)))
        {:client client}
        {:error {:status 401 :body {:error "invalid_client"}}})

      (= "client_secret_post" (:token_endpoint_auth_method client))
      (if (and post-client-id post-client-secret (crypto/check-secret post-client-secret (:client_secret_hash client)))
        {:client client}
        {:error {:status 401 :body {:error "invalid_client"}}})

      :else
      {:error {:status 401 :body {:error "invalid_client"}}})))

(defn- validate-scopes [client requested-scopes]
  (let [allowed (:scopes client)
        requested (set requested-scopes)]
    (or (empty? requested)
        (every? allowed requested))))

(defn- base64url-encode-no-pad [^bytes bytes]
  (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes))

(defn- unsigned-bytes [^java.math.BigInteger number]
  (let [bytes (.toByteArray number)]
    (if (and (> (alength bytes) 1) (zero? (aget bytes 0)))
      (java.util.Arrays/copyOfRange bytes 1 (alength bytes))
      bytes)))

(defn- sign-rs256 [message]
  (let [keypair @oidc-keypair
        private-key ^RSAPrivateKey (.getPrivate keypair)
        signer (Signature/getInstance "SHA256withRSA")]
    (.initSign signer private-key)
    (.update signer (.getBytes message "UTF-8"))
    (.sign signer)))

(defn- build-id-token [client-id user-id]
  (let [now (quot (System/currentTimeMillis) 1000)
        exp (+ now config/id-token-ttl-seconds)
        user (user-model/get-user-by-id user-id)
        role-names (->> (user-model/get-roles-for-user user-id)
                        (map :name)
                        (remove str/blank?)
                        distinct
                        sort
                        vec)
        permission-names (->> (user-model/get-permissions-for-user user-id)
                              (map :name)
                              (remove str/blank?)
                              distinct
                              sort
                              vec)
        claims (cond-> {:iss config/issuer
                        :sub (str (:username user))
                        :user_id (str user-id)
                        :aud client-id
                        :iat now
                        :exp exp
                        :username (:username user)
                        :preferred_username (:username user)
                        :email (:email user)
                        :permissions permission-names
                        :roles role-names}
                 (some? (:first_name user)) (assoc :first_name (:first_name user)
                                                  :given_name (:first_name user))
                 (some? (:last_name user)) (assoc :last_name (:last_name user)
                                                 :family_name (:last_name user)))
        header {:alg "RS256"
                :typ "JWT"
                :kid oidc-key-id}
        encoded-header (base64url-encode-no-pad (.getBytes (json/generate-string header) "UTF-8"))
        encoded-claims (base64url-encode-no-pad (.getBytes (json/generate-string claims) "UTF-8"))
        signing-input (str encoded-header "." encoded-claims)
        signature (base64url-encode-no-pad (sign-rs256 signing-input))]
    (str signing-input "." signature)))

(defn- extract-bearer-token [request]
  (when-let [header (or (get-in request [:headers "authorization"])
                        (get-in request [:headers "Authorization"]))]
    (when (str/starts-with? (str/lower-case header) "bearer ")
      (subs header 7))))

(defn- issue-access-token! [{:keys [client-id user-id grant-type scope]}]
  (let [raw-token (crypto/random-token)
        token-hash (crypto/sha256 raw-token)
        db-token (model/create-access-token {:token_hash token-hash
                                             :client_id client-id
                                             :user_id user-id
                                             :grant_type grant-type
                                             :scope scope
                                             :token_type "Bearer"
                                             :expires_at (now-plus-seconds config/access-token-ttl-seconds)})]
    {:raw raw-token
     :db db-token}))

(defn- issue-refresh-token! [{:keys [access-token-id client-id user-id scope]}]
  (let [raw-token (crypto/random-token)
        token-hash (crypto/sha256 raw-token)
        db-token (model/create-refresh-token {:token_hash token-hash
                                              :access_token_id access-token-id
                                              :client_id client-id
                                              :user_id user-id
                                              :scope scope
                                              :expires_at (now-plus-seconds config/refresh-token-ttl-seconds)})]
    {:raw raw-token
     :db db-token}))

(defn- token-success-body [{:keys [access-token refresh-token id-token scope]}]
  (cond-> {:access_token access-token
           :token_type "Bearer"
           :expires_in (int config/access-token-ttl-seconds)}
    (some? refresh-token) (assoc :refresh_token refresh-token)
    (some? id-token) (assoc :id_token id-token)
    (and (some? scope) (not (str/blank? scope))) (assoc :scope scope)))

(defn- auth-code-grant [request client]
  (let [raw-code (param request :code)
        redirect-uri (param request :redirect_uri)
        code (model/get-active-auth-code (crypto/sha256 raw-code))]
    (cond
      (nil? code)
      {:status 400 :body {:error "invalid_grant"}}

      (not= (:id client) (:client_id code))
      {:status 400 :body {:error "invalid_grant"}}

      (not= redirect-uri (:redirect_uri code))
      {:status 400 :body {:error "invalid_grant"}}

      :else
      (let [scope (:scope code)
            access (issue-access-token! {:client-id (:id client)
                                         :user-id (:user_id code)
                                         :grant-type "authorization_code"
                                         :scope scope})
            refresh (issue-refresh-token! {:access-token-id (:id (:db access))
                                           :client-id (:id client)
                                           :user-id (:user_id code)
                                           :scope scope})
            id-token (build-id-token (:client_id client) (:user_id code))]
        (model/consume-auth-code (:id code))
        {:status 200
         :body (token-success-body {:access-token (:raw access)
                                    :refresh-token (:raw refresh)
                                    :id-token id-token
                                    :scope scope})}))))

(defn- client-credentials-grant [request client]
  (let [scope-list (split-scopes (param request :scope))]
    (cond
      (not (contains? (:grants client) "client_credentials"))
      {:status 400 :body {:error "unauthorized_client"}}

      (not (validate-scopes client scope-list))
      {:status 400 :body {:error "invalid_scope"}}

      :else
      (let [scope (model/scope-string scope-list)
            access (issue-access-token! {:client-id (:id client)
                                         :grant-type "client_credentials"
                                         :scope scope})]
        {:status 200
         :body (token-success-body {:access-token (:raw access)
                                    :scope scope})}))))

(defn- refresh-token-grant [request client]
  (let [raw-refresh (param request :refresh_token)
        refresh (model/get-active-refresh-token (crypto/sha256 raw-refresh))]
    (cond
      (nil? refresh)
      {:status 400 :body {:error "invalid_grant"}}

      (not= (:id client) (:client_id refresh))
      {:status 400 :body {:error "invalid_grant"}}

      :else
      (let [scope (:scope refresh)
            access (issue-access-token! {:client-id (:id client)
                                         :user-id (:user_id refresh)
                                         :grant-type "refresh_token"
                                         :scope scope})
            new-refresh (issue-refresh-token! {:access-token-id (:id (:db access))
                                               :client-id (:id client)
                                               :user-id (:user_id refresh)
                                               :scope scope})]
        (model/revoke-refresh-token (:id refresh))
        (model/mark-refresh-replaced (:id refresh) (:id (:db new-refresh)))
        {:status 200
         :body (token-success-body {:access-token (:raw access)
                                    :refresh-token (:raw new-refresh)
                                    :scope scope})}))))

(defn token [request]
  (let [grant-type (param request :grant_type)
        auth-result (authenticate-client request)]
    (if-let [auth-error (:error auth-result)]
      auth-error
      (let [client (:client auth-result)]
        (cond
          (= "authorization_code" grant-type) (auth-code-grant request client)
          (= "client_credentials" grant-type) (client-credentials-grant request client)
          (= "refresh_token" grant-type) (refresh-token-grant request client)
          :else {:status 400 :body {:error "unsupported_grant_type"}})))))

(defn introspect [request]
  (let [auth-result (authenticate-client request)
        token (param request :token)]
    (if-let [auth-error (:error auth-result)]
      auth-error
      (let [hash (crypto/sha256 token)
            access (model/get-active-access-token hash)
            refresh (model/get-active-refresh-token hash)]
        (cond
          access
          (let [user (when (:user_id access)
                       (user-model/get-user-by-id (:user_id access)))]
            {:status 200
             :body {:active true
                    :client_id (:client_id access)
                    :username (:username user)
                    :scope (:scope access)
                    :token_type "Bearer"
                    :exp (quot (.getTime (:expires_at access)) 1000)
                    :iat (quot (.getTime (:created_at access)) 1000)
                    :sub (when (:user_id access) (str (:username user)))}})

          refresh
          {:status 200
           :body {:active true
                  :client_id (:client_id refresh)
                  :scope (:scope refresh)
                  :exp (quot (.getTime (:expires_at refresh)) 1000)
                  :iat (quot (.getTime (:created_at refresh)) 1000)
                  :sub (str (:user_id refresh))
                  :token_use "refresh_token"}}

          :else
          {:status 200 :body {:active false}})))))

(defn revoke [request]
  (let [auth-result (authenticate-client request)
        token (param request :token)]
    (if-let [auth-error (:error auth-result)]
      auth-error
      (let [client (:client auth-result)
            hash (crypto/sha256 token)
            access-revoked (model/revoke-access-token-by-hash hash (:id client))
            refresh-revoked (model/revoke-refresh-token-by-hash hash (:id client))]
        (log/info "Token revocation counts" {:access access-revoked :refresh refresh-revoked})
        (success 200 "Token revoked or not found")))))

(defn jwks [_]
  (let [keypair @oidc-keypair
        public-key ^RSAPublicKey (.getPublic keypair)]
    {:status 200
     :body {:keys [{:kty "RSA"
                    :kid oidc-key-id
                    :use "sig"
                    :alg "RS256"
                    :n (base64url-encode-no-pad (unsigned-bytes (.getModulus public-key)))
                    :e (base64url-encode-no-pad (unsigned-bytes (.getPublicExponent public-key)))}]}}))

(defn userinfo [request]
  (let [raw-token (extract-bearer-token request)]
    (if (str/blank? raw-token)
      {:status 401 :body {:error "invalid_token"}}
      (if-let [access (model/get-active-access-token (crypto/sha256 raw-token))]
        (if-let [user-id (:user_id access)]
          (let [user (user-model/get-user-by-id user-id)
                roles (->> (user-model/get-roles-for-user user-id)
                           (map :name)
                           (remove str/blank?)
                           distinct
                           sort
                           vec)
                permissions (->> (user-model/get-permissions-for-user user-id)
                                 (map :name)
                                 (remove str/blank?)
                                 distinct
                                 sort
                                 vec)]
            {:status 200
             :body {:sub (str (:username user))
                    :user_id (str user-id)
                    :username (:username user)
                    :preferred_username (:username user)
                    :email (:email user)
                    :first_name (:first_name user)
                    :last_name (:last_name user)
                    :roles roles
                    :permissions permissions}})
          {:status 401 :body {:error "invalid_token"}})
        {:status 401 :body {:error "invalid_token"}}))))

(defn metadata [_]
  {:status 200
   :body {:issuer config/issuer
          :authorization_endpoint (str config/issuer "/oauth/authorize")
          :token_endpoint (str config/issuer "/oauth/token")
          :introspection_endpoint (str config/issuer "/oauth/introspect")
          :revocation_endpoint (str config/issuer "/oauth/revoke")
          :response_types_supported ["code"]
          :grant_types_supported ["authorization_code" "client_credentials" "refresh_token"]
          :token_endpoint_auth_methods_supported ["client_secret_basic" "client_secret_post" "none"]}})

(defn openid-configuration [_]
  {:status 200
   :body {:issuer config/issuer
          :authorization_endpoint (str config/issuer "/oauth/authorize")
          :token_endpoint (str config/issuer "/oauth/token")
          :userinfo_endpoint (str config/issuer "/oauth/userinfo")
          :end_session_endpoint (str config/issuer "/oauth/logout")
          :jwks_uri (str config/issuer "/.well-known/jwks.json")
          :response_types_supported ["code"]
          :grant_types_supported ["authorization_code" "client_credentials" "refresh_token"]
          :id_token_signing_alg_values_supported ["RS256"]
          :subject_types_supported ["public"]
          :scopes_supported ["openid" "profile" "email" "read" "write" "admin"]
          :claims_supported ["sub" "user_id" "username" "preferred_username" "email" "first_name" "last_name" "roles" "permissions" "client_id" "scope"]
          :token_endpoint_auth_methods_supported ["client_secret_basic" "client_secret_post" "none"]}})
