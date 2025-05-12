(ns iam-clj-api.role.view
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [iam-clj-api.role.controller :as controller]
            [ring.util.request :as request]
            [schema.core :as s]))

(defroutes role-view-routes
  (context "/role" []
    :tags ["Role"]
    ;; Get all roles
    (GET "/" []
      :summary "Gets all roles"
      :responses {200 {:schema {:roles [{:id Integer :name String :description (s/maybe String)}]}
                :description "List of roles"}}
      (controller/get-all-roles))

    ;; Get a role by ID
    (GET "/:id" [id]
      :summary "Gets a role by ID"
      :responses {200 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role details"}
                  404 {:description "Role not found"}}
      (controller/get-role-by-id id))

    ;; Create a new role
    (POST "/" request
      :summary "Creates a new role"
      :body [role {:name String :description (s/maybe String)}]
      :responses {201 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role created successfully"}
                  422 {:description "Invalid role data"}}
      (controller/insert-role (get-in request [:body])))

    ;; Update a role
    (PUT "/:id" request
      :summary "Updates a role"
      :body [role {:name (s/maybe String) :description (s/maybe String)}]
      :responses {200 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role updated successfully"}
                  500 {:description "Failed to update role"}}
      (controller/update-role (get-in request [:params :id]) (get-in request [:body])))

    ;; Update a role's name
    (PUT "/:id/name" [id new-name]
      :summary "Updates a role's name"
      :body [name {:name String}]
      :responses {200 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role name updated successfully"}
                  404 {:description "Role not found"}
                  422 {:description "Invalid role name"}}
      (controller/update-role-name id new-name))

    ;; Update a role's description
    (PUT "/:id/description" [id new-description]
      :summary "Updates a role's description"
      :body [description {:description String}]
      :responses {200 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role description updated successfully"}
                  404 {:description "Role not found"}
                  422 {:description "Invalid role description"}}
      (controller/update-role-description id new-description))

    ;; Delete a role
    (DELETE "/:id" [id]
      :summary "Deletes a role"
      :responses {204 {:description "Role deleted successfully"}
                  404 {:description "Role not found"}}
      (controller/delete-role id))

    ;; Get users with a specific role
    (GET "/:id/user" [id]
      :summary "Gets users with a specific role"
      :responses {200 {:schema {:users [{:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}]}
                :description "List of users with the role"}}
      (controller/get-users-with-role id))

    ;; Add a role to a user
    (POST "/:id/user/:user-id" [id user-id]
      :summary "Adds a role to a user"
      :responses {200 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}}
                :description "User with the role added"}
                  404 {:description "Role or user not found"}}
      (controller/add-role-to-user id user-id))

    ;; Add a role to multiple users
    (POST "/:id/users" request
      :summary "Adds a role to multiple users"
      :responses {200 {:schema {:users [{:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}]}
                :description "Users with the role added"}
                  404 {:description "Role or users not found"}}
      (controller/add-role-to-many-users (get-in request [:params :id]) (get-in request [:body :user-ids])))

    ;; Remove a role from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      :summary "Removes a role from a user"
      :responses {204 {:schema {:user {:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}}
                :description "User with the role removed"}
                  404 {:description "Role or user not found"}}
      (controller/remove-role-from-user id user-id))

    ;; Remove a role from multiple users
    (DELETE "/:id/users" request
      :summary "Removes a role from multiple users"
      :responses {204 {:schema {:users [{:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}]}
                :description "Users with the role removed"}
                  404 {:description "Role or users not found"}}
      (controller/remove-role-from-many-users (get-in request [:params :id]) (get-in request [:body :user-ids])))

    ;; Get permissions for a role
    (GET "/:id/permission" [id]
      :summary "Gets permissions for a role"
      :responses {200 {:schema {:permissions [{:id Integer :name String :description (s/maybe String)}]}
                :description "List of permissions for the role"}}
                  404 {:description "Role not found"}
      (controller/get-permissions-for-role id))

    ;; Add a permission to a role
    (POST "/:id/permission/:permission-id" [id permission-id]
      :summary "Adds a permission to a role"
      :body [permission {:permission-id Integer}]
      :responses {200 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role with the permission added"}
                  404 {:description "Role or permission not found"}}
      (controller/add-permission-to-role id permission-id))

    ;; Remove a permission from a role
    (DELETE "/:id/permission/:permission-id" [id permission-id]
      :summary "Removes a permission from a role"
      :responses {204 {:schema {:role {:id Integer :name String :description (s/maybe String)}}
                :description "Role with the permission removed"}
                  404 {:description "Role or permission not found"}}
      (controller/remove-permission-from-role id permission-id))))
