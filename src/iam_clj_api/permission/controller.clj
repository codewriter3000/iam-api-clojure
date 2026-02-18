(ns iam-clj-api.permission.controller
  (:require [lib.core :refer :all]
            [iam-clj-api.permission.model :as model]
            [clojure.tools.logging :as log]
            [lib.response :refer [error success work]]
            [lib.exists :refer [permission-exists? user-exists?]]))

(defn- parse-id [id]
  (try
    (if (integer? id)
      id
      (Integer/parseInt (str id)))
    (catch Exception _
      nil)))

;; Get all permissions
(defn get-all-permissions []
  (log/info "Fetching all permissions")
  (work 200 {:permissions (model/get-all-permissions)}))

;; Get a permission by ID
(defn get-permission-by-id [id]
  (log/info "Fetching permission by ID:" id)
  (let [permission (permission-exists? id)]
    (if permission
      (work 200 {:permission permission})
      (error 404 "Permission not found"))))

;; Get a permission by name
(defn get-permission-by-name [name]
  (log/info "Fetching permission by name:" name)
  (let [permission (model/get-permission-by-name name)]
    (if permission
      (work 200 {:permission permission})
      (error 404 "Permission not found"))))

;; Validate permission input
(defn validate-input [name description]
  (cond
    (empty? name) {:error "Missing name"}
    (> (count (model/get-permission-by-name name)) 0) {:error (str "Permission with name " name " already exists")}
    :else {:name name :description description}))

;; Insert a new permission
(defn insert-permission [name description]
  (log/info "Inserting permission with name:" name)
  (let [validated-input (validate-input name description)]
    (if (:error validated-input)
      (error 400 (:error validated-input))
      (do
        (model/insert-permission validated-input)
        (success 201 "Permission created successfully")))))

;; Update a permission
(defn update-permission [id permission]
  (log/info "Updating permission with ID:" id "Data:" permission)
  (if (permission-exists? id)
    (if (empty? permission)
      (error 400 "Missing permission data")
      (let [result (model/update-permission id permission)
            update-count (:next.jdbc/update-count (first result))]
        (if (= 1 update-count)
          (work 200 {:permission (assoc permission :id (Integer/parseInt (str id)))})
          (error 500 "Failed to update permission"))))
    (error 404 "Permission not found")))

;; Update a permission's name
(defn update-permission-name [id new-name]
  (log/info "Updating permission name for ID:" id)
  (if-let [permission (permission-exists? id)]
    (if (empty? new-name)
      (error 400 "Missing new name")
      (let [result (model/update-permission-name id new-name)]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Permission name updated")
          (error 400 "Failed to update permission name"))))
    (error 404 "Permission not found")))

;; Update a permission's description
(defn update-permission-description [id new-description]
  (log/info "Updating permission description for ID:" id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/update-permission-description id new-description)]
      (if (= 1 (:next.jdbc/update-count (first result)))
        (success 200 "Permission description updated")
        (error 400 "Failed to update permission description")))
    (error 404 "Permission not found")))

;; Delete a permission
(defn delete-permission [id]
  (log/info "Deleting permission with ID:" id)
  (if-let [permission (permission-exists? id)]
    (let [result (model/delete-permission id)]
      (if (= 1 (:delete-count result))
        (success 204 "Permission deleted")
        (error 400 "Failed to delete permission")))
    (error 404 "Permission not found")))

;; Get users with a specific permission
(defn get-users-with-permission [id]
  (log/info "Fetching users with permission ID:" id)
  (if-let [permission (permission-exists? id)]
    (work 200 {:users (model/get-users-with-permission id)})
    (error 404 "Permission not found")))

;; Add a permission to a user
(defn add-permission-to-user [id user-id]
  (log/info "Adding permission ID:" id "to user ID:" user-id)
  (let [permission-id (parse-id id)
        target-user-id (parse-id user-id)]
    (cond
      (or (nil? permission-id) (nil? target-user-id))
      (error 400 "Invalid permission ID or user ID")

      (not (permission-exists? permission-id))
      (error 404 "Permission not found")

      (not (user-exists? target-user-id))
      (error 404 "User not found")

      :else
      (let [result (model/add-permission-to-user permission-id target-user-id)]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 201 "Permission added to user")
          (error 400 "Failed to add permission to user"))))))

;; Remove a permission from a user
(defn remove-permission-from-user [id user-id]
  (log/info "Removing permission ID:" id "from user ID:" user-id)
  (let [permission-id (parse-id id)
        target-user-id (parse-id user-id)]
    (cond
      (or (nil? permission-id) (nil? target-user-id))
      (error 400 "Invalid permission ID or user ID")

      (not (permission-exists? permission-id))
      (error 404 "Permission not found")

      (not (user-exists? target-user-id))
      (error 404 "User not found")

      :else
      (let [result (model/remove-permission-from-user permission-id target-user-id)]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 204 "Permission removed from user")
          (error 400 "Failed to remove permission from user"))))))
