(ns iam-clj-api.user.view
  (:require [compojure.core :refer :all]
            [iam-clj-api.user.controller :as controller]
            [ring.middleware.json :as json]
            [ring.util.request :as request]))

(defroutes user-view-routes
  (context "/user" []
    ;; Get all users
    (GET "/" []
      (controller/get-all-users))

    ;; Get a user by ID
    (GET "/:id" [id]
      (controller/get-user-by-id id))

    ;; Create a new user
    (POST "/" request
      (let [user (get-in request [:body])]
        (controller/insert-user user)))

    ;; User login
    (POST "/login" [username password]
      (controller/login-user username password))

    ;; Update a user
    (PUT "/:id" request
      (let [id (get-in request [:params :id])
            user (get-in request [:body])]
        (controller/update-user id user)))

    ;; Update a user's username
    (PUT "/:id/username" [id new-username]
      (controller/update-user-username id new-username))

    ;; Update a user's email
    (PUT "/:id/email" [id new-email]
      (controller/update-user-email id new-email))

    ;; Update a user's password
    (PUT "/:id/password" [id new-password]
      (controller/update-user-password id new-password))

    ;; Delete a user
    (DELETE "/:id" [id]
      (controller/delete-user id))

    ;; Get permissions for a user
    (GET "/:id/permissions" [id]
      (controller/get-permissions-for-user id))

    ;; Get roles for a user
    (GET "/:id/roles" [id]
      (controller/get-roles-for-user id))

    ;; Add roles to a user
    (POST "/:id/roles" request
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (controller/add-roles-to-user id roles)))

    ;; Remove roles from a user
    (DELETE "/:id/roles" request
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (controller/remove-roles-from-user id roles)))))
