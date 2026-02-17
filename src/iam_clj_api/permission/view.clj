(ns iam-clj-api.permission.view
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [iam-clj-api.permission.controller :as controller]
            [schema.core :as s]))

(defroutes permission-view-routes
  (context "/permission" []
    :tags ["Permission"]
    ;; Get all permissions
    (GET "/" []
      :summary "Gets all permissions"
      :responses {200 {:schema {:permissions [{:id Integer :name String :description (s/maybe String)}]}
                :description "List of permissions"}}
      (controller/get-all-permissions))

    ;; Get a permission by ID
    (GET "/:id" [id]
      :summary "Gets a permission by ID"
      :responses {200 {:schema {:permission {:id Integer :name String :description (s/maybe String)}}
                :description "Permission details"}
                  404 {:description "Permission not found"}}
      (controller/get-permission-by-id id))
    
    ;; Get a permission by name
    (GET "/name/:name" [name]
      :summary "Gets a permission by name"
      :responses {200 {:schema {:permission {:id Integer :name String :description (s/maybe String)}}
                :description "Permission details"}
                  404 {:description "Permission not found"}}
      (controller/get-permission-by-name name))

    ;; Create a new permission
    (POST "/" [name description]
      :summary "Creates a new permission"
      :body [permission {:name String :description (s/maybe String)}]
      (controller/insert-permission name description))

    ;; Update a permission's name
    (PUT "/:id/name" [id new-name]
      :summary "Updates a permission's name"
      :body [name {:name String}]
      :responses {200 {:schema {:permission {:id Integer :name String :description (s/maybe String)}}
                :description "Permission name updated successfully"}
                  404 {:description "Permission not found"}
                  422 {:description "Invalid permission name"}}
      (controller/update-permission-name id new-name))

    ;; Update a permission's description
    (PUT "/:id/description" [id new-description]
      :summary "Updates a permission's description"
      :body [description {:description String}]
      :responses {200 {:schema {:permission {:id Integer :name String :description (s/maybe String)}}
                :description "Permission description updated successfully"}
                  404 {:description "Permission not found"}
                  422 {:description "Invalid permission description"}}
      (controller/update-permission-description id new-description))

    ;; Delete a permission
    (DELETE "/:id" [id]
      :summary "Deletes a permission"
      :responses {204 {:description "Permission deleted successfully"}
                  404 {:description "Permission not found"}}
      (controller/delete-permission id))

    ;; Get users with a specific permission
    (GET "/:id/user" [id]
      :summary "Gets users with a specific permission"
      :responses {200 {:schema {:users [{:id Integer :username String :email String :first_name (s/maybe String) :last_name (s/maybe String)}]}
                :description "List of users with the permission"}}
      (controller/get-users-with-permission id))

    ;; Add a permission to a user
    (POST "/:id/user/:user-id" [id user-id]
      :summary "Adds a permission to a user"
      :responses {200 {:schema {:permission {:id Integer :name String :description (s/maybe String)}}
                :description "Permission added to user successfully"}
                  404 {:description "User or permission not found"}
                  422 {:description "Invalid permission data"}}
      (controller/add-permission-to-user id user-id))

    ;; Remove a permission from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      :summary "Removes a permission from a user"
      :responses {204 {:description "Permission removed from user successfully"}
                  404 {:description "User or permission not found"}}
      (controller/remove-permission-from-user id user-id))))
