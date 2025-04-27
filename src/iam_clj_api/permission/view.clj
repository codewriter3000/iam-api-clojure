(ns iam-clj-api.permission.view
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [iam-clj-api.permission.controller :as controller]))

(defroutes permission-view-routes
  (context "/permission" []
    ;; Get all permissions
    (GET "/" []
      (controller/get-all-permissions))

    ;; Get a permission by ID
    (GET "/:id" [id]
      (controller/get-permission-by-id id))

    ;; Create a new permission
    (POST "/" [name description]
      (controller/insert-permission name description))

    ;; Update a permission's name
    (PUT "/:id/name" [id new-name]
      (controller/update-permission-name id new-name))

    ;; Update a permission's description
    (PUT "/:id/description" [id new-description]
      (controller/update-permission-description id new-description))

    ;; Delete a permission
    (DELETE "/:id" [id]
      (controller/delete-permission id))

    ;; Get users with a specific permission
    (GET "/:id/user" [id]
      (controller/get-users-with-permission id))

    ;; Add a permission to a user
    (POST "/:id/user/:user-id" [id user-id]
      (controller/add-permission-to-user id user-id))

    ;; Remove a permission from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      (controller/remove-permission-from-user id user-id))))