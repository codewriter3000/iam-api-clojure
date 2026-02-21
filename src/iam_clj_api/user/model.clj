(ns iam-clj-api.user.model
  (:require [next.jdbc :as jdbc]
            [lib.core :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

;; User Schema
(def User
  {:id s/Int
   :username s/Str
   :email s/Str
   :first_name (s/maybe s/Str)
   :last_name (s/maybe s/Str)
   :created_at s/Inst})

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
                    force_password_reset BOOLEAN NOT NULL DEFAULT TRUE,
                    reset_token_hash VARCHAR(256),
                    reset_token_expires_at TIMESTAMP,
                    reset_token_used_at TIMESTAMP,
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
(defn get-user-by-id [id]
  (let [query "SELECT id, username, email, first_name, last_name, force_password_reset, created_at FROM users WHERE id = ?;"
        params [(Integer. id)] ; Ensure id is passed as an Integer
          result (jdbc/execute! ds (into [query] params))]
            (log-query query params)
            (log/info "get-user-by-id Result:" result)
            (if (empty? result)
              nil
              (remove-namespace (first result)))))

;; filepath: c:\Users\alex.MICHARSKI\Workspace\iam-clj-api\src\iam_clj_api\user\model.clj
(defn get-all-users []
  (let [query "SELECT id, username, email, first_name, last_name, force_password_reset, created_at FROM users;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
    (map remove-namespace (map #(into {} %) result))))

;; Get a user by username
(defn get-user-by-username [username]
  (let [query "SELECT * FROM users WHERE username = ?;"
        params [username]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Get a user by email
(defn get-user-by-email [email]
  (let [query "SELECT * FROM users WHERE email = ?;"
        params [email]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Get a user by login id (username or email)
(defn get-user-by-login-id [login-id]
  (or (get-user-by-username login-id)
      (get-user-by-email login-id)))

;; Update a user
(defn update-user [id user]
  (if (empty? user)
    (do
      (log/info "No fields to update for user ID:" id)
      {:update-count 0})
    (let [filtered-user (filter-nil-values user)
          set-clause (str/join ", " (map (fn [[k _]] (str (name k) " = ?")) filtered-user))
          values (concat (vals filtered-user) [(Integer. id)])
          query (str "UPDATE users SET " set-clause " WHERE id = ?;")]
      (log-query query values)
      (log/info "update-user query about to be ran...")
      (let [result (jdbc/execute! ds (into [query] values))]
        (log/info "update-user result:" result)
        result))))

;; Update a user's username
(defn update-user-username [id new-username]
  (let [query "UPDATE users SET username = ? WHERE id = ?;"
        params [new-username (Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Update a user's email
(defn update-user-email [id new-email]
  (let [query "UPDATE users SET email = ? WHERE id = ?;"
        params [new-email (Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Update a user's password
(defn update-user-password [id new-password]
  (let [query "UPDATE users SET password = ? WHERE id = ?;"
        params [new-password (Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

(defn set-force-password-reset [id force-password-reset]
  (let [query "UPDATE users SET force_password_reset = ? WHERE id = ?;"
        params [(boolean force-password-reset) (Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Set password reset token and expiry
(defn set-password-reset-token [id token-hash expires-at]
  (let [query "UPDATE users
               SET reset_token_hash = ?, reset_token_expires_at = ?, reset_token_used_at = NULL
               WHERE id = ?;"
        params [token-hash expires-at (Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Get user by valid, unused reset token hash
(defn get-user-by-reset-token-hash [token-hash]
  (let [query "SELECT *
               FROM users
               WHERE reset_token_hash = ?
                 AND reset_token_used_at IS NULL
                 AND reset_token_expires_at > CURRENT_TIMESTAMP;"
        params [token-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (remove-namespace (first result))))

;; Consume password reset token
(defn consume-password-reset-token [id]
  (let [query "UPDATE users
               SET reset_token_hash = NULL,
                   reset_token_expires_at = NULL,
                   reset_token_used_at = CURRENT_TIMESTAMP
               WHERE id = ?;"
        params [(Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Delete a user
(defn delete-user [id]
  (let [query "DELETE FROM users WHERE id = ?;"
        params [(Integer. id)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Get roles for a user
(defn get-roles-for-user [id]
  (let [query "SELECT roles.id, roles.name, roles.description FROM roles
               JOIN users_roles ON roles.id = users_roles.role_id
               WHERE users_roles.user_id = ?;"
        params [(Integer. id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map remove-namespace (map #(into {} %) result))))

;; Get permissions for a user
(defn get-permissions-for-user [id]
  (let [query "SELECT permissions.id, permissions.name, permissions.description FROM permissions
               JOIN users_permissions ON permissions.id = users_permissions.permission_id
               WHERE users_permissions.user_id = ?;"
        params [(Integer. id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map remove-namespace (map #(into {} %) result))))