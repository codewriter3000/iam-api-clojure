(ns iam-clj-api.auth.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.user.controller :as controller]
            [iam-clj-api.schemas :as schemas]))

(defroutes auth-view-routes
  (context "/auth" []
    :tags ["Auth"]
    (POST "/login" request
      :summary "User login"
      :body [credentials schemas/LoginRequest]
      :responses {200 {:schema schemas/LoginResponse
                       :description "Login successful"}
                  401 {:schema schemas/ErrorResponse
                       :description "Invalid credentials"}}
      (let [response (controller/login-user (:login_id credentials) (:password credentials))]
        (if (= 200 (:status response))
          (assoc response :session {:user-id (get-in response [:body :user :id])})
          response)))

    (GET "/session" request
      :summary "Gets currently authenticated session user"
      :responses {200 {:schema schemas/LoginResponse}
                  401 {:schema schemas/ErrorResponse}}
      (controller/get-session-user (get-in request [:session :user-id])))

    (POST "/logout" []
      :summary "Logs out current user"
      :responses {200 {:schema schemas/MessageResponse}}
      {:status 200
       :session nil
       :body {:message "Logged out"}})

    (POST "/password/forget" []
      :summary "Requests a password reset URL"
      :body [payload schemas/ForgotPasswordRequest]
      :responses {200 {:schema schemas/ForgotPasswordResponse}}
      (controller/request-password-reset (:login_id payload)))

    (POST "/password/reset" []
      :summary "Resets password using reset token"
      :body [payload schemas/ResetPasswordRequest]
      :responses {200 {:schema schemas/MessageResponse}
                  400 {:schema schemas/ErrorResponse}}
      (controller/reset-password (:token payload) (:password payload)))))
