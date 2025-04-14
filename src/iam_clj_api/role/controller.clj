(ns iam-clj-api.role.controller
  (:require
   [clojure.tools.logging :as log]
   [iam-clj-api.role.model :as model]
   [lib.core :refer :all]
   [lib.response :refer [error success work]]
   [lib.exists :refer [user-exists? role-exists?]]))

;; Get all roles
(defn get-all-roles []
  (log/info "Fetching all roles")
  (work 200 (model/get-all-roles)))

;; Get a role by ID
(defn get-role-by-id [id]
  (log/info "Fetching role by ID:" id)
  (let [role (role-exists? id)]
    (if role
      (work 200 role)
      (error 404 "Role not found"))))

;; Get a role by name
(defn get-role-by-name [name]
  (log/info "Fetching role by name:" name)
  (let [role (model/get-role-by-name name)]
    (if role
      (work 200 role)
      (error 404 "Role not found"))))

;; Validate role input
(defn validate-input [role]
  (let [name (get role :name)
        description (get role :description)]
    (cond
      (empty? name)
      (error 422 "Missing name")

      (seq (model/get-role-by-name name))
      (error 422 (str "Role with name " name " already exists"))

      :else
      {:name name :description description})))

;; Insert a new role
(defn insert-role [role]
  (log/info "Inserting role:" role)
  (let [validated-input (validate-input role)]
    (if (:error validated-input)
      validated-input
      (do
        (model/insert-role validated-input)
        (success 201 "Role created successfully")))))

;; Update a role
(defn update-role [id role]
  (log/info "Updating role with ID:" id "Data:" role)
  (if (role-exists? id)
    (if (empty? role)
      {:status 400 :error "Missing role data"}
      (let [result (model/update-role id role)]
        (if (= 1 (:update-count result))
          {:status 200 :body "Role updated"}
          {:status 400 :error "Failed to update role"})))
    {:status 404 :error "Role not found"}))

;; Update a role's name
(defn update-role-name [id new-name]
  (log/info "Updating role name for ID:" id)
  (let [role-result (role-exists? id)]
    (log/info "Role result:" role-result)
    (if (role-exists? id)
      (if (empty? new-name)
        {:status 400 :error "Missing new name"}
        (let [result (model/update-role id {:name new-name})]
          (if (= 1 (:next.jdbc/update-count (first result)))
            {:status 200 :body "Role name updated"}
            {:status 400 :error "Failed to update role name"})))
      {:status 404 :error "Role not found"})))

;; Update a role's description
(defn update-role-description [id new-description]
  (log/info "Updating role description for ID:" id)
  (if (role-exists? id)
    (let [result (model/update-role id {:description new-description})]
      (log/info "Result of updating role description:" result)
      (if (= 1 (:next.jdbc/update-count (first result)))
        {:status 200 :body "Role description updated"}
        {:status 400 :error "Failed to update role description"}))
    {:status 404 :error "Role not found"}))

;; Delete a role
(defn delete-role [id]
  (log/info "Deleting role with ID:" id)
  (if (role-exists? id)
    (let [result (model/delete-role id)]
      (if (= 1 (:delete-count result))
        {:status 200 :body "Role deleted"}
        {:status 400 :error "Failed to delete role"}))
    {:status 404 :error "Role not found"}))

;; Get users with a specific role
(defn get-users-with-role [id]
  (log/info "Fetching users with role ID:" id)
  (if (role-exists? id)
    (let [users (model/get-users-with-role id)]
      (log/info "Users with role ID:" id "are:" users)
      (if (nil? users)
        {:status 404 :error "No users found with the given role"}
        {:status 200 :body users}))
    {:status 404 :error "Role not found"}))

;; Add a role to a user
(defn add-role-to-user [role-id user-id]
  (log/info "Adding role ID:" role-id "to user ID:" user-id)
  (if (role-exists? role-id)
    (if (user-exists? user-id)
      (let [result (model/add-role-to-user role-id user-id)
            update-count (:next.jdbc/update-count (first result))] ; Extract the update count
        (log/info "Result of adding role to user:" result)
        (if (= 1 update-count) ; Check the extracted update count
          {:status 200 :body "Role added to user"}
          {:status 400 :error "Failed to add role to user"}))
      {:status 404 :error "User not found"})
    {:status 404 :error "Role not found"}))

;; Add a role to multiple users
(defn add-role-to-many-users [role-id user-ids]
  (log/info "Adding role ID:" role-id "to multiple users")
  (if (role-exists? role-id)
    (let [results (map #(model/add-role-to-user role-id %) user-ids)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count user-ids) success-count)]
      {:status 200 :body {:success-count success-count :failure-count failure-count}})
    {:status 404 :error "Role not found"}))

;; Remove a role from a user
(defn remove-role-from-user [role-id user-id]
  (log/info "Removing role ID:" role-id "from user ID:" user-id)
  (if (user-exists? user-id) ; Check if the user exists first
    (if (role-exists? role-id) ; Then check if the role exists
      (let [result (model/remove-role-from-user role-id user-id)
            update-count (:next.jdbc/update-count (first result))] ; Extract the update count
        (log/info "Result of removing role from user:" result)
        (if (= 1 update-count) ; Check the extracted update count
          {:status 200 :body "Role removed from user"}
          {:status 400 :error "Failed to remove role from user"}))
      {:status 404 :error "Role not found"})
    {:status 404 :error "User not found"})) ; Return user not found if the user doesn't exist

;; Remove a role from multiple users
(defn remove-role-from-many-users [role-id user-ids]
  (log/info "Removing role ID:" role-id "from multiple users")
  (if (role-exists? role-id)
    (let [results (map #(model/remove-role-from-user role-id %) user-ids)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count user-ids) success-count)]
      {:status 200 :body {:success-count success-count :failure-count failure-count}})
    {:status 404 :error "Role not found"}))

;; Get permissions for a role
(defn get-permissions-for-role [id]
  (log/info "Fetching permissions for role ID:" id)
  (if (role-exists? id)
    {:status 200 :body (model/get-permissions-for-role id)}
    {:status 404 :error "Role not found"}))

;; Add a permission to a role
(defn add-permission-to-role [role-id permission-id]
  (log/info "Adding permission ID:" permission-id "to role ID:" role-id)
  (let [result (model/add-permission-to-role role-id permission-id)]
    (if (= 1 (:insert-count result))
      {:status 200 :body "Permission added to role"} ; Return success response
      {:status 400 :error "Failed to add permission to role"}))) ; Return failure response

;; Remove a permission from a role
(defn remove-permission-from-role [permission-id role-id]
  (log/info "Removing permission ID:" permission-id "from role ID:" role-id)
  (if (role-exists? role-id)
    (let [result (model/remove-permission-from-role role-id permission-id)
          update-count (:next.jdbc/update-count (first result))] ; Extract the update count
      (log/info "Result of removing permission from role:" result)
      (if (= 1 update-count) ; Check the extracted update count
        {:status 200 :body "Permission removed from role"}
        {:status 400 :error "Failed to remove permission from role"}))
    {:status 404 :error "Role not found"}))