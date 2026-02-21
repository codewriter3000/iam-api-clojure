(ns iam-clj-api.auth.session
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [iam-clj-api.user.model :as user-model]))

(def public-routes
  #{[:post "/api/auth/login"]
    [:post "/api/auth/password/forget"]
    [:post "/api/auth/password/reset"]})

(def forced-reset-exempt-routes
  #{[:post "/api/auth/login"]
    [:post "/api/auth/password/forget"]
    [:post "/api/auth/password/reset"]
    [:get "/api/auth/password/reset/context"]
    [:post "/api/auth/logout"]})

(defn- swagger-route? [uri]
  (or (str/starts-with? uri "/swagger-ui")
      (= uri "/swagger.json")))

(defn- request-method [request]
  (some-> (:request-method request) name str/lower-case keyword))

(defn- requires-auth? [request]
  (let [uri (:uri request)
        method (request-method request)]
    (and (not= method :options)
         (str/starts-with? uri "/api")
         (not (contains? public-routes [method uri]))
         (not (swagger-route? uri)))))

(defn- force-reset-blocked? [request]
  (let [uri (:uri request)
        method (request-method request)
        session-user-id (get-in request [:session :user-id])]
    (and (str/starts-with? uri "/api")
         session-user-id
         (not (contains? forced-reset-exempt-routes [method uri]))
         (boolean (:force_password_reset (user-model/get-user-by-id session-user-id))))))

(defn wrap-require-session [handler]
  (fn [request]
    (if (and (requires-auth? request)
             (nil? (get-in request [:session :user-id])))
      {:status 401
  :headers {"Content-Type" "application/json"}
  :body (json/generate-string {:error "Authentication required"})}
      (if (force-reset-blocked? request)
        {:status 403
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Password reset required before access is allowed"})}
        (handler request)))))
