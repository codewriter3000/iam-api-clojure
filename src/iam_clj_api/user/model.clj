(ns iam-clj-api.user.model
  (:require [next.jdbc :as jdbc]
            [lib.core :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(def ds (get-datasource))

;; Helper function to filter non-nil values from a map
(defn- filter-nil-values [m]
  (into {} (filter (comp some? val) m)))

;; Helper function to log queries
(defn- log-query [query params]
  (log/info "Executing query:" query "with params:" params))

;; Create the users table
(defn create-user-table []
  (jdbc/execute! ds
                 ["CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(20) NOT NULL,
                    email VARCHAR(256) NOT NULL,
                    first_name VARCHAR(32),
                    last_name VARCHAR(32),
                    password VARCHAR(256) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  );"]))

;; Drop the users table
(defn drop-user-table []
  (jdbc/execute! ds ["DROP TABLE IF EXISTS users;"]))

;; Insert a new user
(defn insert-user [user]
  (let [filtered-user (filter-nil-values user)
        keys (map name (keys filtered-user))
        values (vals filtered-user)
        columns (str/join ", " keys)
        placeholders (str/join ", " (repeat (count values) "?"))
        query (str "INSERT INTO users (" columns ") VALUES (" placeholders ");")]
    (log-query query values)
    (jdbc/execute! ds (into [query] values))))

;; Get all users
(defn get-all-users []
  (let [query "SELECT id, username, email, first_name, last_name FROM users;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
    (map remove-namespace (map #(into {} %) result))))

;; Get a user by ID
(defn get-user-by-id [id]
  (let [query "SELECT id, username, email, first_name, last_name, created_at FROM users WHERE id = ?;"
        params [(Integer/parseInt id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (first result)))

;; Get a user by username
(defn get-user-by-username [username]
  (let [query "SELECT * FROM users WHERE username = ?;"
        params [username]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (remove-namespace (first result))))

;; Update a user
(defn update-user [id user]
  (if (empty? user)
    {:update-count 0}
    (let [filtered-user (filter-nil-values user)
          set-clause (str/join ", " (map (fn [[k _]] (str (name k) " = ?")) filtered-user))
          values (concat (vals filtered-user) [(Integer/parseInt id)])
          query (str "UPDATE users SET " set-clause " WHERE id = ?;")]
      (log-query query values)
      (jdbc/execute! ds (into [query] values)))))

;; Update a user's username
(defn update-user-username [id new-username]
  (let [query "UPDATE users SET username = ? WHERE id = ?;"
        params [new-username (Integer/parseInt id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Update a user's email
(defn update-user-email [id new-email]
  (let [query "UPDATE users SET email = ? WHERE id = ?;"
        params [new-email (Integer/parseInt id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Update a user's password
(defn update-user-password [id new-password]
  (let [query "UPDATE users SET password = ? WHERE id = ?;"
        params [new-password (Integer/parseInt id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Delete a user
(defn delete-user [id]
  (let [query "DELETE FROM users WHERE id = ?;"
        params [(Integer/parseInt id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Get roles for a user
(defn get-roles-for-user [id]
  (let [query "SELECT roles.id, roles.name, roles.description FROM roles
               JOIN users_roles ON roles.id = users_roles.role_id
               WHERE users_roles.user_id = ?;"
        params [(Integer/parseInt id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map remove-namespace (map #(into {} %) result))))

;; Get permissions for a user
(defn get-permissions-for-user [id]
  (let [query "SELECT permissions.id, permissions.name, permissions.description FROM permissions
               JOIN users_permissions ON permissions.id = users_permissions.permission_id
               WHERE users_permissions.user_id = ?;"
        params [(Integer/parseInt id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map remove-namespace (map #(into {} %) result))))