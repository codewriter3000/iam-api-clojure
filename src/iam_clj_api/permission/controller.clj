(ns iam-clj-api.permission.controller
  (:require [lib.core :refer :all]
            [iam-clj-api.permission.model :as model]
            [clojure.tools.logging :as log]))

;; Helper function to check if a permission exists
(defn- permission-exists? [id]
  (let [permission (model/get-permission-by-id id)]
    (if permission
      permission
      nil)))

;; Get all permissions
(defn get-all-permissions []
  (log/info "Fetching all permissions")
  {:status 200 :body (model/get-all-permissions)})

;; Get a permission by ID
(defn get-permission-by-id [id]
  (log/info "Fetching permission by ID:" id)
  (let [permission (permission-exists? id)]
    (if permission
      {:status 200 :body permission}
      {:status 404 :error "Permission not found"})))

;; Validate permission input
(defn validate-input [name description]
  (cond
    (empty? name) {:status 400 :error "Missing name"}
    (> (count (model/get-permission-by-name name)) 0) {:status 400 :error (str "Permission with name " name " already exists")}
    :else {:name name :description description}))

;; Insert a new permission
(defn insert-permission [name description]
  (log/info "Inserting permission with name:" name)
  (let [validated-input (validate-input name description)]
    (if (:error validated-input)
      validated-input
      (do
        (model/insert-permission validated-input)
        {:status 201 :body "Permission created successfully"}))))

;; Update a permission's name
(defn update-permission-name [id new-name]
  (log/info "Updating permission name for ID:" id)
  (if-let [permission (permission-exists? id)]
    (if (empty? new-name)
      {:status 400 :error "Missing new name"}
      (let [result (model/update-permission-name id new-name)]
        (if (= 1 (:update-count result))
          {:status 200 :body "Permission name updated"}
          {:status 400 :error "Failed to update permission name"})))
    {:status 404 :error "Permission not found"}))

;; Update a permission's description
(defn update-permission-description [id new-description]
  (log/info "Updating permission description for ID:" id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/update-permission-description id new-description)]
      (if (= 1 (:update-count result))
        {:status 200 :body "Permission description updated"}
        {:status 400 :error "Failed to update permission description"}))
    {:status 404 :error "Permission not found"}))

;; Delete a permission
(defn delete-permission [id]
  (log/info "Deleting permission with ID:" id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/delete-permission id)]
      (if (= 1 (:delete-count result))
        {:status 200 :body "Permission deleted"}
        {:status 400 :error "Failed to delete permission"}))
    {:status 404 :error "Permission not found"}))

;; Get users with a specific permission
(defn get-users-with-permission [id]
  (log/info "Fetching users with permission ID:" id)
  (if-let [permission (permission-exists? id)]
    {:status 200 :body (model/get-users-with-permission id)}
    {:status 404 :error "Permission not found"}))

;; Add a permission to a user
(defn add-permission-to-user [id user-id]
  (log/info "Adding permission ID:" id "to user ID:" user-id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/add-permission-to-user id user-id)]
      (if (= 1 (:insert-count result))
        {:status 200 :body "Permission added to user"}
        {:status 400 :error "Failed to add permission to user"}))
    {:status 404 :error "Permission not found"}))

;; Remove a permission from a user
(defn remove-permission-from-user [id user-id]
  (log/info "Removing permission ID:" id "from user ID:" user-id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/remove-permission-from-user id user-id)]
      (if (= 1 (:delete-count result))
        {:status 200 :body "Permission removed from user"}
        {:status 400 :error "Failed to remove permission from user"}))
    {:status 404 :error "Permission not found"}))