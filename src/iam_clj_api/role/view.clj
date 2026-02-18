(ns iam-clj-api.role.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.role.controller :as controller]
            [iam-clj-api.schemas :as schemas]
            [clojure.tools.logging :as log]))

(defroutes role-view-routes
  (context "/role" []
    :tags ["Role"]
    ;; Get all roles
    (GET "/" []
      :summary "Gets all roles"
      :responses {200 {:schema schemas/RolesResponse
                       :description "List of roles"}}
      (controller/get-all-roles))

    ;; Get a role by ID
    (GET "/:id" [id]
      :summary "Gets a role by ID"
       :responses {200 {:schema schemas/RoleWithUsersResponse
                       :description "Role details"}
             404 {:schema schemas/ErrorResponse
               :description "Role not found"}}
      (controller/get-role-by-id id))

    ;; Create a new role
    (POST "/" [role]
      :summary "Creates a new role"
      :body [role schemas/CreateRoleRequest]
      :responses {201 {:schema schemas/MessageResponse
                       :description "Role created successfully"}
                  422 {:schema schemas/ErrorResponse
                       :description "Invalid role data"}}
      (do
        (log/info "Creating new role with data:" role)
        (controller/insert-role role)))

    ;; Update a role
    (PUT "/:id" [id]
      :summary "Updates a role"
      :body [role schemas/RoleUpdateRequest]
      :responses {200 {:schema schemas/RoleWithUsersResponse
                       :description "Role updated successfully"}
                  500 {:schema schemas/ErrorResponse
                       :description "Failed to update role"}}
      (do
        (log/info "Updating role with ID:" id "Data:" role)
        (controller/update-role id role)))

    ;; Update a role's name
    (PUT "/:id/name" [id new-name]
      :summary "Updates a role's name"
       :body [name schemas/NamePayload]
       :responses {200 {:schema schemas/MessageResponse
                       :description "Role name updated successfully"}
             404 {:schema schemas/ErrorResponse
               :description "Role not found"}
             422 {:schema schemas/ErrorResponse
               :description "Invalid role name"}}
       (controller/update-role-name id (:name name)))

    ;; Update a role's description
    (PUT "/:id/description" [id new-description]
      :summary "Updates a role's description"
       :body [description schemas/DescriptionPayload]
       :responses {200 {:schema schemas/MessageResponse
                       :description "Role description updated successfully"}
             404 {:schema schemas/ErrorResponse
               :description "Role not found"}
             422 {:schema schemas/ErrorResponse
               :description "Invalid role description"}}
       (controller/update-role-description id (:description description)))

    ;; Delete a role
    (DELETE "/:id" [id]
      :summary "Deletes a role"
       :responses {204 {:schema schemas/MessageResponse
               :description "Role deleted successfully"}
             404 {:schema schemas/ErrorResponse
               :description "Role not found"}}
      (controller/delete-role id))

    ;; Get users with a specific role
    (GET "/:id/user" [id]
      :summary "Gets users with a specific role"
      :responses {200 {:schema schemas/UsersResponse
                       :description "List of users with the role"}}
      (controller/get-users-with-role id))

    ;; Add a role to a user
    (POST "/:id/user/:user-id" [id user-id]
      :summary "Adds a role to a user"
       :responses {200 {:schema schemas/MessageResponse
               :description "Role added to user"}
             404 {:schema schemas/ErrorResponse
               :description "Role or user not found"}}
      (controller/add-role-to-user id user-id))

    ;; Add a role to multiple users
    (POST "/:id/users" [id]
      :summary "Adds a role to multiple users"
      :body [payload schemas/UsersPayload]
      :responses {200 {:schema schemas/CountPayload
                       :description "Users with the role added"}
                  404 {:schema schemas/ErrorResponse
                       :description "Role or users not found"}}
      (controller/add-role-to-many-users id (:user-ids payload)))

    ;; Remove a role from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      :summary "Removes a role from a user"
       :responses {204 {:schema schemas/MessageResponse
               :description "Role removed from user"}
             404 {:schema schemas/ErrorResponse
               :description "Role or user not found"}}
      (controller/remove-role-from-user id user-id))

    ;; Remove a role from multiple users
    (DELETE "/:id/users" [id]
      :summary "Removes a role from multiple users"
      :body [payload schemas/UsersPayload]
      :responses {200 {:schema schemas/CountPayload
                       :description "Users with the role removed"}
                  404 {:schema schemas/ErrorResponse
                       :description "Role or users not found"}}
      (controller/remove-role-from-many-users id (:user-ids payload)))

    ;; Get permissions for a role
    (GET "/:id/permission" [id]
      :summary "Gets permissions for a role"
      :responses {200 {:schema schemas/PermissionsResponse
                       :description "List of permissions for the role"}}
                  404 {:schema schemas/ErrorResponse
                       :description "Role not found"}
      (controller/get-permissions-for-role id))

    ;; Add a permission to a role
    (POST "/:id/permission/:permission-id" [id permission-id]
      :summary "Adds a permission to a role"
       :responses {200 {:schema schemas/MessageResponse
               :description "Permission added to role"}
             404 {:schema schemas/ErrorResponse
               :description "Role or permission not found"}}
      (controller/add-permission-to-role id permission-id))

    ;; Remove a permission from a role
    (DELETE "/:id/permission/:permission-id" [id permission-id]
      :summary "Removes a permission from a role"
       :responses {204 {:schema schemas/MessageResponse
               :description "Permission removed from role"}
             404 {:schema schemas/ErrorResponse
               :description "Role or permission not found"}}
      (controller/remove-permission-from-role id permission-id))))
