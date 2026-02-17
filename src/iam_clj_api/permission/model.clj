(ns iam-clj-api.permission.model
  (:require [next.jdbc :as jdbc]
            [lib.core :refer :all]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

;; Permission Schema
(def Permission
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)})

(def ds (get-datasource))

(defn- as-int [value]
  (try
    (if (integer? value)
      value
      (Integer/parseInt (str value)))
    (catch Exception _
      nil)))

;; Helper function to log queries
(defn- log-query [query params]
  (log/info "Executing query:" query "with params:" params))

(defn- log-result [result]
  (log/info "Query result:" result))

;; Create the permissions table
(defn create-permission-table []
  (jdbc/execute! ds
                 ["CREATE TABLE IF NOT EXISTS permissions (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(128) NOT NULL,
                    description VARCHAR(1000)
                  );

                   CREATE TABLE IF NOT EXISTS users_permissions (
                    user_id INT NOT NULL,
                    permission_id INT NOT NULL
                  );"]))

;; Drop the permissions table
(defn drop-permission-table []
  (jdbc/execute! ds ["DROP TABLE IF EXISTS permissions; DROP TABLE IF EXISTS users_permissions;"]))

;; Insert a new permission
(defn insert-permission [permission]
  (let [query "INSERT INTO permissions (name, description) VALUES (?, ?);"
        params [(get permission :name) (get permission :description)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Get all permissions
(defn get-all-permissions []
  (let [query "SELECT id, name, description FROM permissions;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
      (map remove-namespace (map #(into {} %) result))))

;; Get a permission by ID
(defn get-permission-by-id [id]
  (let [query "SELECT id, name, description FROM permissions WHERE id = ?;"
  params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Get a permission by name
(defn get-permission-by-name [name]
  (let [query "SELECT id, name, description FROM permissions WHERE name = ?;"
        params [name]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Update a permission's name
(defn update-permission-name [id new-name]
  (let [query "UPDATE permissions SET name = ? WHERE id = ?;"
  params [new-name (as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    {:update-count (:next.jdbc/update-count (first result))}))

;; Update a permission's description
(defn update-permission-description [id new-description]
  (let [query "UPDATE permissions SET description = ? WHERE id = ?;"
  params [new-description (as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    {:update-count (:next.jdbc/update-count (first result))}))

;; Delete a permission
(defn delete-permission [id]
  (let [query "DELETE FROM permissions WHERE id = ?;"
  params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    {:delete-count (:next.jdbc/update-count (first result))}))

;; Get users with a specific permission
(defn get-users-with-permission [id]
  (let [query "SELECT u.id, u.username, u.email
               FROM users u
               JOIN users_permissions up ON u.id = up.user_id
               WHERE up.permission_id = ?;"
        params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (log-result result)
    (map remove-namespace (map #(into {} %) result))))

;; Add a permission to a user
(defn add-permission-to-user [id user-id]
  (let [query "INSERT INTO users_permissions (user_id, permission_id) VALUES (?, ?);"
  params [(as-int user-id) (as-int id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Remove a permission from a user
(defn remove-permission-from-user [id user-id]
  (let [query "DELETE FROM users_permissions WHERE user_id = ? AND permission_id = ?;"
  params [(as-int user-id) (as-int id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))
