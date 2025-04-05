(ns iam-clj-api.permission.view
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [iam-clj-api.permission.controller :as controller]
            [ring.util.response :refer [response]]))

(defn- validate-param [param param-name]
  (if param
    true
    {:status 400 :body (str "Missing " param-name)}))

(defroutes permission-view-routes
  (context "/permission" []
    ;; Get all permissions
    (GET "/" []
      (response (controller/get-all-permissions)))

    ;; Get a permission by ID
    (GET "/:id" [id]
      (let [valid (validate-param id "permission ID")]
        (if (= valid true)
          (response (controller/get-permission-by-id id))
          valid)))

    ;; Create a new permission
    (POST "/" [name description]
      (if (and name description)
        (response (controller/insert-permission name description))
        {:status 400 :body "Missing name or description"}))

    ;; Update a permission's name
    (PUT "/:id/name" [id new-name]
      (if (and id new-name)
        (response (controller/update-permission-name id new-name))
        {:status 400 :body "Missing permission ID or new name"}))

    ;; Update a permission's description
    (PUT "/:id/description" [id new-description]
      (if (and id new-description)
        (response (controller/update-permission-description id new-description))
        {:status 400 :body "Missing permission ID or new description"}))

    ;; Delete a permission
    (DELETE "/:id" [id]
      (let [valid (validate-param id "permission ID")]
        (if (= valid true)
          (response (controller/delete-permission id))
          valid)))

    ;; Get users with a specific permission
    (GET "/:id/user" [id]
      (let [valid (validate-param id "permission ID")]
        (if (= valid true)
          (response (controller/get-users-with-permission id))
          valid)))

    ;; Add a permission to a user
    (POST "/:id/user/:user-id" [id user-id]
      (if (and id user-id)
        (response (controller/add-permission-to-user id user-id))
        {:status 400 :body "Missing permission ID or user ID"}))

    ;; Remove a permission from a user
    (DELETE "/:id/user/:user-id" [id user-id]
      (if (and id user-id)
        (response (controller/remove-permission-from-user id user-id))
        {:status 400 :body "Missing permission ID or user ID"}))))