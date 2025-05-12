(ns iam-clj-api.user.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.user.controller :as controller]
            [ring.middleware.json :as json]
            [ring.util.request :as request]
            [schema.core :as s]
            [clojure.tools.logging :as log]))

(defroutes user-view-routes
  (context "/user" []
    :tags ["User"]
    ;; Get all users
    (GET "/" []
      :summary "Gets all users"
      :responses {200 {:schema {:users [{:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String) :created_at s/Inst}]}
                :description "List of users"}}
      (controller/get-all-users))

    ;; Get a user by ID
    (GET "/:id" [id]
      :summary "Gets a user by ID"
      :responses {200 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String) :created_at s/Inst}}
                :description "User details"}
                  404 {:description "User not found"}}
      (controller/get-user-by-id id))

    ;; Create a new user
    (POST "/" request
      :summary "Creates a new user"
      :body [user {:username String :email String :password String :first_name (s/maybe String) :last_name (s/maybe String)}]
      :responses {201 {:schema {:message String}}
                  422 {:schema {:message String}}}
      (let [user (get-in request [:body-params])] ; Use :body-params to correctly extract the request body
        (controller/insert-user user)))

    ;; User login
    (POST "/login" [username password]
      :summary "User login"
      :body [credentials {:username String :password String}]
      :responses {200 {:schema {:token String}
                :description "Login successful"}
                  401 {:description "Invalid credentials"}}
      (controller/login-user username password))

    ;; Update a user
    (PUT "/:id" [id]
      :summary "Updates a user"
      :body [user {(s/optional-key :username) String
                   (s/optional-key :email) String
                   (s/optional-key :first_name) String
                   (s/optional-key :last_name) String}]
      :responses {200 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String) :created_at s/Inst}}}
                  404 {:schema {:error String}}
                  500 {:schema {:error String}}}
      (do
        (log/info "Updating user with ID:" id "Data:" user)
        (controller/update-user id user)))

    ;; Update a user's password
    (PUT "/:id/password" [id new-password]
      :summary "Updates a user's password"
      :body [password {:password String}]
      :responses {200 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String) :created_at s/Inst}}
                :description "Password updated successfully"}
                  404 {:schema {:error String}}
                  500 {:schema {:error String}}}
      (controller/update-user-password id new-password))

    ;; Delete a user
    (DELETE "/:id" [id]
      :summary "Deletes a user"
      :responses {204 {:schema {:message String}}
                  404 {:schema {:error String}}
                  500 {:schema {:error String}}}
      (controller/delete-user id))

    ;; Get permissions for a user
    (GET "/:id/permissions" [id]
      :summary "Gets permissions for a user"
      :responses {200 {:schema {:permissions [{:id Integer :name String :description (s/maybe String)}]}
                :description "List of permissions"}}
                  404 {:description "User not found"}
      (controller/get-permissions-for-user id))

    ;; Get roles for a user
    (GET "/:id/roles" [id]
      :summary "Gets roles for a user"
      :responses {200 {:schema {:roles [{:id Integer :name String :description (s/maybe String)}]}
                :description "List of roles"}}
                  404 {:description "User not found"}
      (controller/get-roles-for-user id))

    ;; Add roles to a user
    (POST "/:id/roles" request
      :summary "Adds roles to a user"
      :body [roles [{:id Integer :name String :description (s/maybe String)}]]
      :responses {200 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}}
                :description "Roles added successfully"}
                  400 {:schema {:error String}}
                  404 {:schema {:error String}}
                  422 {:schema {:error String}}
                  500 {:schema {:error String}}}
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (controller/add-roles-to-user id roles)))

    ;; Remove roles from a user
    (DELETE "/:id/roles" request
      :summary "Removes roles from a user"
      :responses {204 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}}
                :description "Roles removed successfully"}
                  404 {:schema {:error String}}
                  500 {:schema {:error String}}}
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (controller/remove-roles-from-user id roles)))))
