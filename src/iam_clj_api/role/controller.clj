(ns iam-clj-api.role.controller
  (:require
   [clojure.tools.logging :as log]
   [iam-clj-api.role.model :as model]
   [lib.core :refer :all]
   [lib.response :refer [error success work]]
   [lib.exists :refer [user-exists? role-exists? permission-exists?]]))

;; Get all roles
(defn get-all-roles []
  (log/info "Fetching all roles")
  (let [roles (map remove-namespace (model/get-all-roles))
        result (work 200 {:roles roles})]
    (log/info "Response: " result)
    result))

;; Get a role by ID
(defn get-role-by-id [id]
  (log/info "Fetching role by ID:" id)
  (let [role (role-exists? id)]
    (if role
      (let [users (model/get-users-with-role id)]
        (log/info "Users with role ID:" id "are:" users)
        (work 200 {:role (assoc role :users users)}))
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
      (error 400 "Missing role data")
      (let [basic-info-result (model/update-role id (dissoc role :users))
            users-result (model/sync-role-users id (:users role))]
        (if (and (= 1 (:next.jdbc/update-count (first basic-info-result)))
                  (map? users-result)
                  (contains? users-result :added)
                  (contains? users-result :removed)
                  (set? (:added users-result))
                  (set? (:removed users-result)))
          (do (log/info "Updated role: " {:role (assoc role :id (Integer. id))})
          (work 200 {:role (assoc role :id (Integer. id))}))
          (do (log/info "Basic Info Result: " (:next.jdbc/update-count (first basic-info-result)))
              (log/info "Users Result: " users-result)
              (error 500 "Failed to update role")))))
    (error 404 "Role not found")))

;; Update a role's name
(defn update-role-name [id new-name]
  (log/info "Updating role name for ID:" id)
  (let [role-result (role-exists? id)]
    (log/info "Role result:" role-result)
    (if (role-exists? id)
      (if (empty? new-name)
        (error 400 "Missing new name")
        (let [result (model/update-role id {:name new-name})]
          (if (= 1 (:next.jdbc/update-count (first result)))
            (success 200 "Role name updated")
            (error 400 "Failed to update role name"))))
      (error 404 "Role not found"))))

;; Update a role's description
(defn update-role-description [id new-description]
  (log/info "Updating role description for ID:" id)
  (if (role-exists? id)
    (let [result (model/update-role id {:description new-description})]
      (log/info "Result of updating role description:" result)
      (if (= 1 (:next.jdbc/update-count (first result)))
        (success 200 "Role description updated")
        (error 400 "Failed to update role description")))
    (error 404 "Role not found")))

;; Delete a role
(defn delete-role [id]
  (log/info "Deleting role with ID:" id)
  (if (role-exists? id)
    (let [result (model/delete-role id)]
      (if (= 1 (:delete-count result))
        (success 204 "Role deleted successfully")
        (error 400 "Failed to delete role")))
    (error 404 "Role not found")))

;; Get users with a specific role
(defn get-users-with-role [id]
  (log/info "Fetching users with role ID:" id)
  (if (role-exists? id)
    (let [users (model/get-users-with-role id)]
      (log/info "Users with role ID:" id "are:" users)
      (if (nil? users)
        (error 404 "No users found with the given role")
        (work 200 users)))
  (error 404 "Role not found")))

;; Add a role to a user
(defn add-role-to-user [role-id user-id]
  (log/info "Adding role ID:" role-id "to user ID:" user-id)
  (if (role-exists? role-id)
    (if (user-exists? user-id)
      (let [result (model/add-role-to-user role-id user-id)
            update-count (:next.jdbc/update-count (first result))] ; Extract the update count
        (log/info "Result of adding role to user:" result)
        (if (= 1 update-count) ; Check the extracted update count
          (success 200 "Role added to user") ; Return success response
          (error 400 "Failed to add role to user"))) ; Return failure response
      (error 404 "User not found")) ; Return user not found if the user doesn't exist
    (error 404 "Role not found"))) ; Return role not found if the role doesn't exist

;; Add a role to multiple users
(defn add-role-to-many-users [role-id user-ids]
  (log/info "Adding role ID:" role-id "to multiple users")
  (if (role-exists? role-id)
    (let [results (map #(model/add-role-to-user role-id %) user-ids)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count user-ids) success-count)]
      (work 200 {:success-count success-count :failure-count failure-count}))
    (error 404 "Role not found")))

;; Remove a role from a user
(defn remove-role-from-user [role-id user-id]
  (log/info "Removing role ID:" role-id "from user ID:" user-id)
  (if (user-exists? user-id) ; Check if the user exists first
    (if (role-exists? role-id) ; Then check if the role exists
      (let [result (model/remove-role-from-user role-id user-id)
            update-count (:next.jdbc/update-count (first result))] ; Extract the update count
        (log/info "Result of removing role from user:" result)
        (if (= 1 update-count) ; Check the extracted update count
          (success 204 "Role removed from user") ; Return success response
          (error 400 "Failed to remove role from user"))) ; Return failure response
      (error 404 "Role not found")) ; Return role not found if the role doesn't exist
    (error 404 "User not found"))) ; Return user not found if the user doesn't exist

;; Remove a role from multiple users
(defn remove-role-from-many-users [role-id user-ids]
  (log/info "Removing role ID:" role-id "from multiple users")
  (if (role-exists? role-id)
    (let [results (map #(model/remove-role-from-user role-id %) user-ids)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count user-ids) success-count)]
      (work 200
            {:success-count success-count :failure-count failure-count}))
    (error 404 "Role not found")))

;; Get permissions for a role
(defn get-permissions-for-role [id]
  (log/info "Fetching permissions for role ID:" id)
  (if (role-exists? id)
    (work 200 (model/get-permissions-for-role id))
    (error 404 "Role not found")))

;; Add a permission to a role
(defn add-permission-to-role [role-id permission-id]
  (log/info "Adding permission ID:" permission-id "to role ID:" role-id)
  (if (role-exists? role-id)
    (if (permission-exists? permission-id)
      (let [result (model/add-permission-to-role role-id permission-id)]
        (if (= 1 (:insert-count result))
          (success 200 "Permission added to role") ; Return success response
          (error 400 "Failed to add permission to role"))) ; Return failure response
      (error 404 "Permission not found")) ; Return permission not found if the permission doesn't exist
    (error 404 "Role not found"))) ; Return role not found if the role doesn't exist

;; Remove a permission from a role
(defn remove-permission-from-role [permission-id role-id]
  (log/info "Removing permission ID:" permission-id "from role ID:" role-id)
  (if (role-exists? role-id)
    (if (permission-exists? permission-id)
      (let [result (model/remove-permission-from-role role-id permission-id)
            update-count (when (seq result) (:next.jdbc/update-count (first result)))]
        (log/info "Result of removing permission from role:" result)
        (if (= 1 update-count)
          (success 204 "Permission removed from role")
          (error 400 "Failed to remove permission from role")))
      (error 404 "Permission not found"))
    (error 404 "Role not found")))