(ns iam-clj-api.role.model
  (:require [next.jdbc :as jdbc]
            [lib.core :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

;; Role Schema
(def Role
  {:id s/Int
   :name s/Str
   :description (s/maybe s/Str)})

(def ds (get-datasource))

;; Helper function to filter non-nil values from a map
(defn- filter-nil-values [m]
  (into {} (filter (comp some? val) m)))

;; Helper function to log queries
(defn- log-query [query params]
  (log/info "Executing query:" query "with params:" params))

;; Create the roles table
(defn create-role-table []
  (jdbc/execute! ds
                 ["CREATE TABLE IF NOT EXISTS roles (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(128) NOT NULL,
                    description VARCHAR(1000)
                  );

                   CREATE TABLE IF NOT EXISTS users_roles (
                    user_id INT NOT NULL,
                    role_id INT NOT NULL
                  );

                   CREATE TABLE IF NOT EXISTS roles_permissions (
                    role_id INT NOT NULL,
                    permission_id INT NOT NULL
                  );"]))

;; Drop the roles table
(defn drop-role-table []
  (jdbc/execute! ds ["DROP TABLE IF EXISTS roles;
                      DROP TABLE IF EXISTS users_roles;
                      DROP TABLE IF EXISTS roles_permissions;"]))

;; Insert a new role
(defn insert-role [role]
  (let [query "INSERT INTO roles (name, description) VALUES (?, ?);"
        params [(get role :name) (get role :description)]]
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Get all roles
(defn get-all-roles []
  (let [query "SELECT id, name, description FROM roles;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
    (if (empty? result)
      nil
      (map remove-namespace (map #(into {} %) result)))))

;; Get a role by ID
(defn get-role-by-id [id]
  (let [query "SELECT id, name, description FROM roles WHERE id = ?;"
        params [id]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Get a role by name
(defn get-role-by-name [name]
  (let [query "SELECT id, name, description FROM roles WHERE name = ?;"
        params [name]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (remove-namespace (first result)))))

;; Update a role
(defn update-role [id role]
  (if (empty? role)
    {:update-count 0}
    (let [filtered-role (filter-nil-values role)
          set-clause (str/join ", " (map (fn [[k _]] (str (name k) " = ?")) filtered-role))
          values (concat (vals filtered-role) [(Integer. id)])
          query (str "UPDATE roles SET " set-clause " WHERE id = ?;")]
      (log-query query values)
      (jdbc/execute! ds (into [query] values)))))

;; Delete a role
(defn delete-role [id]
  (let [query "DELETE FROM roles WHERE id = ?;"
        params [(Integer. id)]]
          (log-query query params)
          (try
            (jdbc/execute! ds (into [query] params))
            (do
            (log/info "Role deleted successfully for ID:" id)
            {:delete-count 1}) ; Return success if the query executes
            (catch Exception e
            (log/error e "Failed to delete role for ID:" id)
            {:delete-count 0})))) ; Return failure if an exception occurs

;; Get users with a specific role
(defn get-users-with-role [id]
  (let [query "SELECT u.id, u.username, u.email
               FROM users u
               JOIN users_roles ur ON u.id = ur.user_id
               WHERE ur.role_id = ?;"
        params [(Integer. id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (map remove-namespace (map #(into {} %) result)))))

;; Add a role to a user
(defn add-role-to-user [role-id user-id]
  (let [query "INSERT INTO users_roles (role_id, user_id) VALUES (?, ?);"
        params [(Integer. role-id) (Integer. user-id)]] ; Ensure both parameters are integers
    (log-query query params)
    (jdbc/execute! ds (into [query] params))))

;; Remove a role from a user
(defn remove-role-from-user [role-id user-id]
  (let [query "DELETE FROM users_roles WHERE role_id = ? AND user_id = ?;"
        params [(Integer. role-id) (Integer. user-id)]]
    (log-query query params)
    (let [result (jdbc/execute! ds (into [query] params))]
      (log/info "Query result:" result)
      result)))

;; Get permissions for a role
(defn get-permissions-for-role [id]
  (let [query "SELECT p.id, p.name, p.description
               FROM permissions p
               JOIN roles_permissions rp ON p.id = rp.permission_id
               WHERE rp.role_id = ?;"
        params [(Integer. id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (if (empty? result)
      nil
      (map remove-namespace (map #(into {} %) result)))))

;; Add a permission to a role
(defn add-permission-to-role [permission-id role-id]
  (let [query "INSERT INTO roles_permissions (role_id, permission_id) VALUES (?, ?);"
        params [(Integer. role-id) (Integer. permission-id)]] ; Ensure both parameters are integers
    (log-query query params)
    (try
      (jdbc/execute! ds (into [query] params))
      {:insert-count 1} ; Return success if the query executes
      (catch Exception e
        (log/error e "Failed to add permission to role")
        {:insert-count 0})))) ; Return failure if an exception occurs

;; Remove a permission from a role
(defn remove-permission-from-role [role-id permission-id]
  (let [query "DELETE FROM roles_permissions WHERE role_id = ? AND permission_id = ?;"
        params [(Integer. role-id) (Integer. permission-id)]] ; Ensure both parameters are integers
    (log-query query params)
    (let [result (jdbc/execute! ds (into [query] params))]
      (log/info "Query result:" result)
      result)))