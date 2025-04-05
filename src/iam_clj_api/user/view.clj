(ns iam-clj-api.user.view
  (:require [compojure.core :refer :all]
            [iam-clj-api.user.controller :as controller]
            [ring.middleware.json :as json]
            [ring.util.request :as request]
            [ring.util.response :refer [response]]))

(defroutes user-view-routes
  (context "/user" []
    ;; Get all users
    (GET "/" []
      (response
       (controller/get-all-users)))

    ;; Get a user by ID
    (GET "/:id" [id]
      (if id
        (response (controller/get-user-by-id id))
        {:status 400 :body "Missing user ID"}))

    ;; Create a new user
    (POST "/" request
      (let [user (get-in request [:body])]
        (if user
          (do
            (controller/insert-user user)
            {:status 201 :body "User created"})
          {:status 400 :body "Invalid user data"})))

    ;; User login
    (POST "/login" [username password]
      (if (and username password)
        (response (controller/login-user username password))
        {:status 400 :body "Missing username or password"}))

      ;; Update a user
    (PUT "/:id" request
      (let [id (get-in request [:params :id])
            user (get-in request [:body])]
        (if (and id user)
          (do
            (controller/update-user id user)
            {:status 200 :body "User updated"})
          {:status 400 :body "Missing user ID or data"})))

    ;; Update a user's username
    (PUT "/:id/username" [id new-username]
      (if (and id new-username)
        (response (controller/update-user-username id new-username))
        {:status 400 :body "Missing user ID or new username"}))

    ;; Update a user's email
    (PUT "/:id/email" [id new-email]
      (if (and id new-email)
        (response (controller/update-user-email id new-email))
        {:status 400 :body "Missing user ID or new email"}))

    ;; Update a user's password
    (PUT "/:id/password" [id new-password]
      (if (and id new-password)
        (response (controller/update-user-password id new-password))
        {:status 400 :body "Missing user ID or new password"}))

    ;; Delete a user
    (DELETE "/:id" [id]
      (if id
        (do
          (controller/delete-user id)
          {:status 200 :body "User deleted"})
        {:status 400 :body "Missing user ID"}))

    ;; Get permissions for a user
    (GET "/:id/permissions" [id]
      (if id
        (response (controller/get-permissions-for-user id))
        {:status 400 :body "Missing user ID"}))

    ;; Get roles for a user
    (GET "/:id/roles" [id]
      (if id
        (response (controller/get-roles-for-user id))
        {:status 400 :body "Missing user ID"}))

    ;; Add roles to a user
    (POST "/:id/roles" request
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (if (and id roles)
          (do
            (controller/add-roles-to-user id roles)
            {:status 200 :body "Roles added to user"})
          {:status 400 :body "Missing user ID or roles"})))

    ;; Remove roles from a user
    (DELETE "/:id/roles" request
      (let [id (get-in request [:params :id])
            roles (get-in request [:body :roles])]
        (if (and id roles)
          (do
            (controller/remove-roles-from-user id roles)
            {:status 200 :body "Roles removed from user"})
          {:status 400 :body "Missing user ID or roles"})))))