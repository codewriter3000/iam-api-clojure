(ns iam-clj-api.role.view
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [iam-clj-api.role.controller :as controller]
            [ring.util.request :as request]))

(defroutes role-view-routes
  (context "/role" []
    ;; Get all roles
    (GET "/" []
      (controller/get-all-roles))

    ;; Get a role by ID
    (GET "/:id" [id]
      (controller/get-role-by-id id))

    ;; Create a new role
    (POST "/" request
      (controller/insert-role (get-in request [:body])))

    ;; Update a role
    (PUT "/:id" request
      (controller/update-role (get-in request [:params :id]) (get-in request [:body])))

    ;; Update a role's name
    (PUT "/:id/name" [id new-name]
      (controller/update-role-name id new-name))

    ;; Update a role's description
    (PUT "/:id/description" [id new-description]
      (controller/update-role-description id new-description))

    ;; Delete a role
    (DELETE "/:id" [id]
      (controller/delete-role id))

    ;; Get users with a specific role
    (GET "/:id/user" [id]
      (controller/get-users-with-role id))

    ;; Add a role to a user
    (POST "/:id/user/:user-id" [id user-id]
      (controller/add-role-to-user id user-id))

    ;; Add a role to multiple users
    (POST "/:id/users" request
      (controller/add-role-to-many-users (get-in request [:params :id]) (get-in request [:body :user-ids])))

    ;; Remove a role from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      (controller/remove-role-from-user id user-id))

    ;; Remove a role from multiple users
    (DELETE "/:id/users" request
      (controller/remove-role-from-many-users (get-in request [:params :id]) (get-in request [:body :user-ids])))

    ;; Get permissions for a role
    (GET "/:id/permission" [id]
      (controller/get-permissions-for-role id))

    ;; Add a permission to a role
    (POST "/:id/permission/:permission-id" [id permission-id]
      (controller/add-permission-to-role id permission-id))

    ;; Remove a permission from a role
    (DELETE "/:id/permission/:permission-id" [id permission-id]
      (controller/remove-permission-from-role id permission-id))))
