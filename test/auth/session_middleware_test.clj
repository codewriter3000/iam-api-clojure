(ns auth.session-middleware-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.auth.session :as session]))

(def ok-handler (fn [_] {:status 200 :body {:message "ok"}}))

(deftest blocks-protected-route-without-session
  (let [handler (session/wrap-require-session ok-handler)
        response (handler {:request-method :get :uri "/api/user/"})]
    (is (= 401 (:status response)))
    (is (= "Authentication required" (get-in response [:body :error])))))

(deftest allows-public-login-route-without-session
  (let [handler (session/wrap-require-session ok-handler)
        response (handler {:request-method :post :uri "/api/user/login"})]
    (is (= 200 (:status response)))))

(deftest allows-protected-route-with-session
  (let [handler (session/wrap-require-session ok-handler)
        response (handler {:request-method :get :uri "/api/user/" :session {:user-id 1}})]
    (is (= 200 (:status response)))))
