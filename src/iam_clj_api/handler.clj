(ns iam-clj-api.handler
  (:require [compojure.route :as route]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload :refer [wrap-reload]]
            [iam-clj-api.user.view :as user-view]
            [iam-clj-api.permission.view :as permission-view]
            [iam-clj-api.role.view :as role-view]
            [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET routes]]))

;(defn wrap-no-anti-forgery [handler]
;  (wrap-defaults handler (assoc-in site-defaults [:security :anti-forgery] false)))

(defn log-request [handler]
  (fn [request]
    (log/info "Handling request:" request)
    (let [response (handler request)]
      (log/info "Response:" response)
      response)))

(defn wrap-json-response [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Content-Type"] "application/json"))))

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
            (wrap-error-handling
             (context "/api" []
               (GET "/" [] {:status 200 :body "API is running"})
               user-view/user-view-routes
               permission-view/permission-view-routes
               role-view/role-view-routes)))
           (wrap-json-response)))
      ;; Middleware
      (wrap-resource "public") ; Serve all static files from "resources/public"
      (wrap-cors :access-control-allow-origin [#"http://localhost:3000"]
                 :access-control-allow-methods [:get :put :post :delete]
                 :access-control-allow-headers ["Content-Type" "Authorization"])
      log-request))

(defrecord HttpServerComponent [config] component/Lifecycle

           (start [component]
             (log/info "Starting HTTP server")
             (let [handler (-> app
                               wrap-reload
                               keyword-params/wrap-keyword-params
                               params/wrap-params
                               wrap-custom-middleware)
                   server (jetty/run-jetty handler {:port 8080 :join? false})]
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