(ns iam-clj-api.handler
  (:require [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.resource :refer [wrap-resource]]
            [iam-clj-api.user.view :as user-view]
            [iam-clj-api.permission.view :as permission-view]
            [iam-clj-api.role.view :as role-view]
            [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [api context GET routes]]))

(defn wrap-no-anti-forgery [handler]
  (wrap-defaults handler (assoc-in site-defaults [:security :anti-forgery] false)))

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

(def app
  (-> (routes
       ;; Serve Swagger UI as static files
       (route/resources "/swagger-ui" {:root "swagger-ui"}) ; Serve files from "resources/public/swagger-ui"
       ;; Define API routes
       (api
        {:swagger {:ui "/swagger-ui/index.html"
                   :spec "/swagger.json"
                   :data {:info {:title "IAM API"
                                 :description "API for managing users, roles, and permissions"}
                          :tags [{:name "users" :description "User-related endpoints"}
                                 {:name "roles" :description "Role-related endpoints"}
                                 {:name "permissions" :description "Permission-related endpoints"}]}}}
        (context "/api" []
          :tags ["API"]
          (GET "/" [] {:status 200 :body "API is running"})
          user-view/user-view-routes
          permission-view/permission-view-routes
          role-view/role-view-routes)))
      ;; Middleware
      (wrap-resource "public") ; Serve all static files from "resources/public"
      (wrap-cors :access-control-allow-origin [#"http://localhost:3000"]
                 :access-control-allow-methods [:get :put :post :delete]
                 :access-control-allow-headers ["Content-Type" "Authorization"])
      log-request))