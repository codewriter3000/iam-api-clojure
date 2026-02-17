(ns iam-clj-api.user.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.user.controller :as controller]
            [iam-clj-api.schemas :as schemas]
            [clojure.tools.logging :as log]))

(defroutes user-view-routes
  (context "/user" []
    :tags ["User"]
    ;; Get all users
    (GET "/" []
      :summary "Gets all users"
      :responses {200 {:schema schemas/UsersResponse
                :description "List of users"}}
      (controller/get-all-users))

    ;; Get a user by ID
    (GET "/:id" [id]
      :summary "Gets a user by ID"
      :responses {200 {:schema schemas/UserResponse
                :description "User details"}
                  404 {:description "User not found"}}
      (controller/get-user-by-id id))

    ;; Create a new user
    (POST "/" [user]
      :summary "Creates a new user"
      :body [user schemas/CreateUserRequest]
      :responses {201 {:schema schemas/MessageResponse}
                  422 {:schema schemas/ErrorResponse}}
      (controller/insert-user user))

    ;; User login
    (POST "/login" [credentials]
      :summary "User login"
      :body [credentials schemas/LoginRequest]
      :responses {200 {:schema schemas/MessageResponse
                :description "Login successful"}
                  401 {:schema schemas/ErrorResponse
                       :description "Invalid credentials"}}
      (controller/login-user (:username credentials) (:password credentials)))

    ;; Update a user
    (PUT "/:id" [id]
      :summary "Updates a user"
      :body [user schemas/UpdateUserRequest]
      :responses {200 {:schema schemas/UserResponse}
                  404 {:schema schemas/ErrorResponse}
                  500 {:schema schemas/ErrorResponse}}
      (do
        (log/info "Updating user with ID:" id "Data:" user)
        (controller/update-user id user)))

    ;; Update a user's password
    (PUT "/:id/password" [id password]
      :summary "Updates a user's password"
      :body [password schemas/PasswordPayload]
      :responses {200 {:schema schemas/MessageResponse
                :description "Password updated successfully"}
                  404 {:schema schemas/ErrorResponse}
                  500 {:schema schemas/ErrorResponse}}
      (controller/update-user-password id (:password password)))

    ;; Delete a user
    (DELETE "/:id" [id]
      :summary "Deletes a user"
      :responses {204 {:schema schemas/MessageResponse}
                  404 {:schema schemas/ErrorResponse}
                  500 {:schema schemas/ErrorResponse}}
      (controller/delete-user id))

    ;; Get permissions for a user
    (GET "/:id/permissions" [id]
      :summary "Gets permissions for a user"
      :responses {200 {:schema schemas/PermissionsResponse
                       :description "List of permissions"}
                  404 {:schema schemas/ErrorResponse
                       :description "User not found"}}
      (controller/get-permissions-for-user id))

    ;; Get roles for a user
    (GET "/:id/roles" [id]
      :summary "Gets roles for a user"
      :responses {200 {:schema schemas/RolesResponse
                       :description "List of roles"}
                  404 {:schema schemas/ErrorResponse
                       :description "User not found"}}
      (controller/get-roles-for-user id))

    ;; Add roles to a user
    (POST "/:id/roles" [id]
      :summary "Adds roles to a user"
      :body [payload schemas/RolesPayload]
      :responses {200 {:schema schemas/CountMessageResponse
                :description "Roles added successfully"}
                  400 {:schema schemas/ErrorResponse}
                  404 {:schema schemas/ErrorResponse}
                  422 {:schema schemas/ErrorResponse}
                  500 {:schema schemas/ErrorResponse}}
      (controller/add-roles-to-user id (:roles payload)))

    ;; Remove roles from a user
    (DELETE "/:id/roles" [id]
      :summary "Removes roles from a user"
      :body [payload schemas/RolesPayload]
      :responses {200 {:schema schemas/CountMessageResponse
                :description "Roles removed successfully"}
                  404 {:schema schemas/ErrorResponse}
                  500 {:schema schemas/ErrorResponse}}
      (controller/remove-roles-from-user id (:roles payload)))))
