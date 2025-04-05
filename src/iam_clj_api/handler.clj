(ns iam-clj-api.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-body]]
            [iam-clj-api.user.view :as user-view]
            [iam-clj-api.permission.view :as permission-view]
            [iam-clj-api.role.view :as role-view]
            [env :as env]
            [clojure.tools.logging :as log]
            [ring.swagger.swagger2 :as rs]))

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

(def app-routes
  (routes
   (wrap-no-anti-forgery (GET "/" [] "API is running"))
  (rs/swagger-ui "/swagger-ui" :swagger-docs "/swagger-docs")
   (wrap-no-anti-forgery user-view/user-view-routes)
   (wrap-no-anti-forgery permission-view/permission-view-routes)
   (wrap-no-anti-forgery role-view/role-view-routes)
   (route/not-found {:status 404 :body "Not Found"})))

(def app
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#"http://localhost:3000"]
                 :access-control-allow-methods [:get :put :post :delete]
                 :access-control-allow-headers ["Content-Type" "Authorization"])
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-json-response
      log-request))