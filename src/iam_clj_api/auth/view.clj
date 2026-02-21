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
             403 {:schema schemas/ErrorResponse
               :description "Password reset required"}
                  401 {:schema schemas/ErrorResponse
                       :description "Invalid credentials"}}
      (let [response (controller/login-user (:login_id credentials) (:password credentials))]
        (if (= 200 (:status response))
          (let [user (get-in response [:body :user])
                requires-password-reset (= "Password reset required" (get-in response [:body :message]))]
            (assoc response :session {:user-id (:id user)
                                      :username (:username user)
                                      :first-name (:first_name user)
                                      :last-name (:last_name user)
                                      :permissions (:permissions user)
                                      :force-reset-authorized requires-password-reset}))
          response)))

    (GET "/session" request
      :summary "Gets currently authenticated session user"
      :responses {200 {:schema schemas/LoginResponse}
                  403 {:schema schemas/ErrorResponse}
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

    (GET "/password/reset/context" request
      :summary "Checks whether the current session is allowed to complete forced password reset"
      :responses {200 {:schema schemas/MessageResponse}
                  401 {:schema schemas/ErrorResponse}
                  403 {:schema schemas/ErrorResponse}}
      (let [user-id (get-in request [:session :user-id])
            authorized? (boolean (get-in request [:session :force-reset-authorized]))]
        (cond
          (nil? user-id)
          {:status 401
           :body {:error "Authentication required"}}

          (and authorized? (controller/password-reset-required? user-id))
          {:status 200
           :body {:message "Password reset required"}}

          :else
          {:status 403
           :body {:error "No forced password reset is pending"}})))

    (POST "/password/reset" request
      :summary "Resets password using reset token"
      :body [payload schemas/ResetPasswordRequest]
      :responses {200 {:schema schemas/LoginResponse}
                  403 {:schema schemas/ErrorResponse}
                  400 {:schema schemas/ErrorResponse}}
      (let [response (controller/reset-password (:token payload)
                                                (:password payload)
                                                (get-in request [:session :user-id])
                                                (boolean (get-in request [:session :force-reset-authorized])))]
        (if (= 200 (:status response))
          (let [user (get-in response [:body :user])]
            (assoc response :session {:user-id (:id user)
                                      :username (:username user)
                                      :first-name (:first_name user)
                                      :last-name (:last_name user)
                                      :permissions (:permissions user)}))
          response)))))
