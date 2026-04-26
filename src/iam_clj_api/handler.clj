(ns iam-clj-api.handler
  (:require [compojure.route :as route]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload :refer [wrap-reload]]
            [iam-clj-api.auth.view :as auth-view]
            [iam-clj-api.user.view :as user-view]
            [iam-clj-api.permission.view :as permission-view]
            [iam-clj-api.role.view :as role-view]
            [iam-clj-api.oauth.view :as oauth-view]
            [iam-clj-api.oauth.admin.view :as oauth-admin-view]
            [iam-clj-api.auth.session :as auth-session]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET routes]]))

;(defn wrap-no-anti-forgery [handler]
;  (wrap-defaults handler (assoc-in site-defaults [:security :anti-forgery] false)))

(defn log-request [handler]
  (fn [request]
    (log/info "Handling request:" request)
    (let [response (handler request)]
      (if (<= 400 (or (:status response) 0) 599)
        (log/error "Response:" response)
        (log/info "Response:" response))
      response)))

(defn wrap-json-response [handler]
  (fn [request]
    (let [response (handler request)]
      (if (some? (:body response))
        (assoc-in response [:headers "Content-Type"] "application/json")
        response))))

(defn wrap-error-handling [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Error handling request")
        {:status 500
         :body {:error "Internal Server Error"}}))))

(defn wrap-custom-middleware [handler]
  (fn [request]
    (log/info "UPDATE REQUEST IN MIDDLEWARE")
    (let [response (handler request)]
      (log/info "UPDATE RESPONSE IN MIDDLEWARE")
      response)))

(defn- frontend-base-url []
  (or (System/getenv "FRONTEND_BASE_URL")
      "http://localhost:3000"))

(defn- frontend-origin []
  (let [base-url (frontend-base-url)]
    (try
      (let [uri (java.net.URI. base-url)
            scheme (.getScheme uri)
            host (.getHost uri)
            port (.getPort uri)]
        (if (and (some? scheme) (some? host))
          (str scheme "://" host (when (pos? port) (str ":" port)))
          base-url))
      (catch Exception _
        base-url))))

(defn- cors-allowed-origins []
  (let [configured (some-> (System/getenv "CORS_ALLOWED_ORIGINS")
                           (str/split #","))
        defaults ["http://localhost:3000"
                  "https://localhost:3000"
                  (frontend-origin)]]
    (->> (concat configured defaults)
         (map str)
         (map str/trim)
         (remove str/blank?)
         distinct
         (map #(re-pattern (java.util.regex.Pattern/quote %)))
         vec)))

(defn- env-true? [v]
  (contains? #{"true" "1" "yes" "on"}
             (some-> v str str/trim str/lower-case)))

(defn- session-cookie-attrs []
  (let [same-site-env (some-> (System/getenv "SESSION_COOKIE_SAME_SITE")
                              str/trim
                              str/lower-case)
        same-site (case same-site-env
                    "none" :none
                    "strict" :strict
                    "lax" :lax
                    :lax)
        secure? (if-let [secure-env (System/getenv "SESSION_COOKIE_SECURE")]
                  (env-true? secure-env)
                  false)]
    {:http-only true
     :same-site same-site
     :secure secure?}))

(def app
  (-> (routes
       ;; Serve Swagger UI as static files
       (route/resources "/swagger-ui" {:root "swagger-ui"}) ; Serve files from "resources/public/swagger-ui"
       ;; Define API routes
       (-> (api
            {:swagger {:ui "/swagger-ui/index.html"
                       :spec "/swagger.json"
                       :data {:info {:title "IAM API"
                                     :description "API for managing users, roles, and permissions"}}}}
            (context "/api" []
              (GET "/" [] {:status 200 :body "API is running"})
              auth-view/auth-view-routes
              user-view/user-view-routes
              permission-view/permission-view-routes
              role-view/role-view-routes
              oauth-admin-view/oauth-admin-view-routes)
            oauth-view/oauth-view-routes)
           (wrap-json-response)))
      ;; Middleware
      (wrap-resource "public") ; Serve all static files from "resources/public"
      wrap-error-handling
      keyword-params/wrap-keyword-params
      params/wrap-params
      auth-session/wrap-require-session
      (wrap-session {:store (memory-store)
                     :cookie-name "iam-session"
                     :cookie-attrs (session-cookie-attrs)})
      log-request
      (wrap-cors :access-control-allow-origin (cors-allowed-origins)
                 :access-control-allow-methods [:get :put :post :delete :options]
                 :access-control-allow-headers ["Content-Type" "Authorization" "Cookie"]
                 :access-control-allow-credentials "true")))

(defrecord HttpServerComponent [config] component/Lifecycle

           (start [component]
             (log/info "Starting HTTP server")
             (let [handler (-> app
                               wrap-reload
                               wrap-custom-middleware)
                   server (jetty/run-jetty handler {:port 8001 :join? false})]
               (assoc component :server server)))

           (stop [component]
             (log/info "Stopping HTTP server")
             (when-let [server (:server component)]
               (.stop server))
             (assoc component :server nil)))

(defn new-http-server-component [config]
  (let [server (map->HttpServerComponent {:config config})]
    (component/start server)))

(defn -main [& args]
  (new-http-server-component {}))