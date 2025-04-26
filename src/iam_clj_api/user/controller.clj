(ns iam-clj-api.user.controller
  (:require [lib.core :refer :all]
            [iam-clj-api.user.model :as model]
            [iam-clj-api.role.model :as role-model]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [lib.response :refer [error success work]]
            [lib.exists :refer [user-exists?]]))

;; Validate user input
(defn validate-input [user]
  (let [username (get user :username)
        email (get user :email)
        password (get user :password)]
    (cond
      (or (empty? username) (empty? email) (empty? password))
      {:error "All fields are required"}

      (or (< (count username) 3) (> (count username) 20))
      {:error "Username must be between 3 and 20 characters"}

      (not (re-matches #".+@.+\..+" email))
      {:error "Email is invalid"}

      (seq (model/get-user-by-username username))
      {:error "Username already exists"}

      :else
      (assoc user :password (hashers/derive (get user :password))))))

;; Insert a new user
(defn insert-user [user]
  (log/info "Inserting user:" user)
  (let [validated-input (validate-input user)]
    (if (:error validated-input)
      (error 422 (:error validated-input))
      (do
        (model/insert-user validated-input)
        (success 201 "User created successfully")))))

;; Login a user
(defn login-user [username password]
  (log/info "Logging in user:" username)
  (let [user (model/get-user-by-username username)]
    (if (and user (hashers/check password (get user :password)))
      (success 200 "Login successful")
      (error 401 "Invalid username or password"))))

;; Get all users
(defn get-all-users []
  (log/info "Fetching all users")
  (work 200 (map remove-namespace (model/get-all-users))))

;; Get a user by ID
(defn get-user-by-id [id]
  (log/info "Fetching user by ID:" id)
  (let [user (user-exists? id)]
    (if user
      (work 200 user)
      (error 404 "User not found"))))

;; Update a user
(defn update-user [id user]
  (log/info "Updating user with ID:" id "Data:" user)
  (let [existing-user (user-exists? id)]
    (if existing-user
      (let [result (model/update-user id user)]
        (if (= 1 (:update-count result))
          (success 200 "User updated successfully")
          (error 400 "Failed to update user")))
      (error 404 "User not found"))))

;; Update a user's username
(defn update-user-username [id new-username]
  (log/info "Updating username for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/update-user id {:username new-username})]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Username updated successfully")
          (error 400 "Failed to update username")))
      (error 404 "User not found"))))

;; Update a user's email
(defn update-user-email [id new-email]
  (log/info "Updating email for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/update-user id {:email new-email})]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Email updated successfully")
          (error 400 "Failed to update email")))
      (error 404 "User not found"))))

;; Update a user's password
(defn update-user-password [id new-password]
  (log/info "Updating password for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/update-user-password id (hashers/derive new-password))]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Password updated successfully")
          (error 400 "Failed to update password")))
      (error 404 "User not found"))))

;; Delete a user
(defn delete-user [id]
  (log/info "Deleting user with ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/delete-user id)]
        (if result
          (success 204 "User deleted successfully")
          (error 400 "Failed to delete user")))
      (error 404 "User not found"))))

;; Get roles for a user
(defn get-roles-for-user [id]
  (log/info "Fetching roles for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (work 200 (model/get-roles-for-user id))
      {:status 404 :error "User not found"})))

;; Add roles to a user
(defn add-roles-to-user [id roles]
  (log/info "Adding roles to user ID:" id "Roles:" roles)
  (let [user (user-exists? id)]
    (if user
      (let [results (map #(role-model/add-role-to-user % id) roles)
            success-count (count (filter #(= 1 (:update-count %)) results))
            failure-count (- (count roles) success-count)]
        (success 200 {:success-count success-count :failure-count failure-count}))
      (error 404 "User not found"))))

;; Remove roles from a user
(defn remove-roles-from-user [id roles]
  (log/info "Removing roles from user ID:" id "Roles:" roles)
  (let [user (user-exists? id)]
    (if user
      (let [results (map #(role-model/remove-role-from-user id %) roles)
            success-count (count (filter #(= 1 (:update-count %)) results))
            failure-count (- (count roles) success-count)]
        (success 200 {:success-count success-count :failure-count failure-count}))
      (error 404 "User not found"))))

;; Get permissions for a user
(defn get-permissions-for-user [id]
  (log/info "Fetching permissions for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (work 200 (model/get-permissions-for-user id))
      {:status 404 :error "User not found"})))