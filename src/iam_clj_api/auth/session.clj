(ns iam-clj-api.auth.session
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(def public-routes
  #{[:post "/api/auth/login"]
    [:post "/api/auth/password/forget"]
    [:post "/api/auth/password/reset"]})

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

(defn wrap-require-session [handler]
  (fn [request]
    (if (and (requires-auth? request)
             (nil? (get-in request [:session :user-id])))
      {:status 401
  :headers {"Content-Type" "application/json"}
  :body (json/generate-string {:error "Authentication required"})}
      (handler request))))
