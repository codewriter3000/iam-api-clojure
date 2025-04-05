(ns iam-clj-api.role.view
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [iam-clj-api.role.controller :as controller]
            [ring.util.request :as request]
            [ring.util.response :refer [response]]))

(defn- validate-param [param param-name]
  (if param
    true
    {:status 400 :body (str "Missing " param-name)}))

(defroutes role-view-routes
  (context "/role" []
    ;; Get all roles
    (GET "/" []
      (response (controller/get-all-roles)))

    ;; Get a role by ID
    (GET "/:id" [id]
      (if-let [valid (validate-param id "role ID")]
        (response (controller/get-role-by-id id))
        valid))

    ;; Create a new role
    (POST "/" request
      (let [role (get-in request [:body])]
        (if role
          (do
            (controller/insert-role role)
            {:status 201 :body "Role created"})
          {:status 400 :body "Invalid role data"})))

    ;; Update a role
    (PUT "/:id" request
      (let [id (get-in request [:params :id])
            role (get-in request [:body])]
        (if (and id role)
          (do
            (controller/update-role id role)
            {:status 200 :body "Role updated"})
          {:status 400 :body "Missing role ID or data"})))

    ;; Update a role's name
    (PUT "/:id/name" [id new-name]
      (if (and id new-name)
        (response (controller/update-role-name id new-name))
        {:status 400 :body "Missing role ID or new name"}))

    ;; Update a role's description
    (PUT "/:id/description" [id new-description]
      (if (and id new-description)
        (response (controller/update-role-description id new-description))
        {:status 400 :body "Missing role ID or new description"}))

    ;; Delete a role
    (DELETE "/:id" [id]
      (if-let [valid (validate-param id "role ID")]
        (do
          (controller/delete-role id)
          {:status 200 :body "Role deleted"})
        valid))

    ;; Get users with a specific role
    (GET "/:id/user" [id]
      (if-let [valid (validate-param id "role ID")]
        (response (controller/get-users-with-role id))
        valid))

    ;; Add a role to a user
    (POST "/:id/user/:user-id" [id user-id]
      (if (and id user-id)
        (do
          (controller/add-role-to-user id user-id)
          {:status 200 :body "Role added to user"})
        {:status 400 :body "Missing role ID or user ID"}))

    ;; Add a role to multiple users
    (POST "/:id/users" request
      (let [id (get-in request [:params :id])
            user-ids (get-in request [:body :user-ids])]
        (if (and id user-ids)
          (do
            (controller/add-role-to-many-users id user-ids)
            {:status 200 :body "Role added to users"})
          {:status 400 :body "Missing role ID or user IDs"})))

    ;; Remove a role from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      (if (and id user-id)
        (do
          (controller/remove-role-from-user id user-id)
          {:status 200 :body "Role removed from user"})
        {:status 400 :body "Missing role ID or user ID"}))

    ;; Remove a role from multiple users
    (DELETE "/:id/users" request
      (let [id (get-in request [:params :id])
            user-ids (get-in request [:body :user-ids])]
        (if (and id user-ids)
          (do
            (controller/remove-role-from-many-users id user-ids)
            {:status 200 :body "Role removed from users"})
          {:status 400 :body "Missing role ID or user IDs"})))

    ;; Get permissions for a role
    (GET "/:id/permission" [id]
      (if-let [valid (validate-param id "role ID")]
        (response (controller/get-permissions-for-role id))
        valid))

    ;; Add a permission to a role
    (POST "/:id/permission/:permission-id" [id permission-id]
      (if (and id permission-id)
        (do
          (controller/add-permission-to-role id permission-id)
          {:status 200 :body "Permission added to role"})
        {:status 400 :body "Missing role ID or permission ID"}))

    ;; Remove a permission from a role
    (DELETE "/:id/permission/:permission-id" [id permission-id]
      (if (and id permission-id)
        (do
          (controller/remove-permission-from-role id permission-id)
          {:status 200 :body "Permission removed from role"})
        {:status 400 :body "Missing role ID or permission ID"}))))