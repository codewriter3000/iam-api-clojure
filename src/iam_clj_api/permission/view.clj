(ns iam-clj-api.permission.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.permission.controller :as controller]
            [iam-clj-api.schemas :as schemas]))

(defroutes permission-view-routes
  (context "/permission" []
    :tags ["Permission"]
    ;; Get all permissions
    (GET "/" []
      :summary "Gets all permissions"
      :responses {200 {:schema schemas/PermissionsResponse
                :description "List of permissions"}}
      (controller/get-all-permissions))

    ;; Get a permission by ID
    (GET "/:id" [id]
      :summary "Gets a permission by ID"
      :responses {200 {:schema schemas/PermissionResponse
                :description "Permission details"}
                  404 {:schema schemas/ErrorResponse
                       :description "Permission not found"}}
      (controller/get-permission-by-id id))
    
    ;; Get a permission by name
    (GET "/name/:name" [name]
      :summary "Gets a permission by name"
      :responses {200 {:schema schemas/PermissionResponse
                :description "Permission details"}
                  404 {:schema schemas/ErrorResponse
                       :description "Permission not found"}}
      (controller/get-permission-by-name name))

    ;; Create a new permission
    (POST "/" [permission]
      :summary "Creates a new permission"
      :body [permission schemas/CreatePermissionRequest]
      :responses {201 {:schema schemas/MessageResponse}
                  400 {:schema schemas/ErrorResponse}}
      (controller/insert-permission (:name permission) (:description permission)))

        ;; Update a permission
        (PUT "/:id" [id]
       :summary "Updates a permission"
       :body [permission schemas/PermissionUpdateRequest]
       :responses {200 {:schema schemas/PermissionResponse
               :description "Permission updated successfully"}
             400 {:schema schemas/ErrorResponse
               :description "Missing permission data"}
             404 {:schema schemas/ErrorResponse
               :description "Permission not found"}
             500 {:schema schemas/ErrorResponse
               :description "Failed to update permission"}}
       (controller/update-permission id permission))

    ;; Update a permission's name
    (PUT "/:id/name" [id name]
      :summary "Updates a permission's name"
      :body [name schemas/NamePayload]
      :responses {200 {:schema schemas/MessageResponse
                :description "Permission name updated successfully"}
                  404 {:schema schemas/ErrorResponse
                       :description "Permission not found"}
                  422 {:schema schemas/ErrorResponse
                       :description "Invalid permission name"}}
      (controller/update-permission-name id (:name name)))

    ;; Update a permission's description
    (PUT "/:id/description" [id description]
      :summary "Updates a permission's description"
      :body [description schemas/DescriptionPayload]
      :responses {200 {:schema schemas/MessageResponse
                :description "Permission description updated successfully"}
                  404 {:schema schemas/ErrorResponse
                       :description "Permission not found"}
                  422 {:schema schemas/ErrorResponse
                       :description "Invalid permission description"}}
      (controller/update-permission-description id (:description description)))

    ;; Delete a permission
    (DELETE "/:id" [id]
      :summary "Deletes a permission"
       :responses {204 {:schema schemas/MessageResponse
               :description "Permission deleted successfully"}
             404 {:schema schemas/ErrorResponse
               :description "Permission not found"}}
      (controller/delete-permission id))

    ;; Get users with a specific permission
    (GET "/:id/user" [id]
      :summary "Gets users with a specific permission"
      :responses {200 {:schema schemas/UsersResponse
                :description "List of users with the permission"}}
      (controller/get-users-with-permission id))

    ;; Add a permission to a user
    (POST "/:id/user/:user-id" [id user-id]
      :summary "Adds a permission to a user"
       :responses {201 {:schema schemas/MessageResponse
               :description "Permission added to user successfully"}
             404 {:schema schemas/ErrorResponse
               :description "User or permission not found"}
             422 {:schema schemas/ErrorResponse
               :description "Invalid permission data"}}
      (controller/add-permission-to-user id user-id))

    ;; Remove a permission from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      :summary "Removes a permission from a user"
       :responses {204 {:schema schemas/MessageResponse
               :description "Permission removed from user successfully"}
             404 {:schema schemas/ErrorResponse
               :description "User or permission not found"}}
      (controller/remove-permission-from-user id user-id))))
