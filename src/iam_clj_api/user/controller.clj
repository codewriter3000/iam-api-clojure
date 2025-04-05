(ns iam-clj-api.user.controller
  (:require [lib.core :refer :all]
            [iam-clj-api.user.model :as model]
            [iam-clj-api.role.model :as role-model]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [lib.response :refer [error success work]]))

;; Helper function to check if a user exists
(defn- user-exists? [id]
  (let [user (model/get-user-by-id id)]
    (if user
      user
      (error 404 "User not found"))))

;; Validate user input
(defn validate-input [user]
  (let [username (get user :username)
        email (get user :email)
        password (get user :password)]
    (cond
      (not (re-matches #"(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}" password))
      (error 400 "Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a number, and a special character")

      (or (< (count username) 3) (> (count username) 20))
      (error 400 "Username must be between 3 and 20 characters")

      (not (re-matches #".+@.+\..+" email))
      (error 400 "Email is invalid")

      (not (empty? (model/get-user-by-username username)))
      (error 400 "Username already exists")

      :else
      (assoc user :password (hashers/derive (get user :password))))))

;; Insert a new user
(defn insert-user [user]
  (log/info "Inserting user:" user)
  (let [validated-user (validate-input user)]
    (if (not= 400 (:status validated-user))
      (do
        (model/insert-user validated-user)
        (success 201 "User created successfully"))
      validated-user)))

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
  (let [users (model/get-all-users)]
    (work 200 (map remove-namespace users))))

;; Get a user by ID
(defn get-user-by-id [id]
  (log/info "Fetching user by ID:" id)
  (if-let [user (user-exists? id)]
    (work 200 user)
    user))

;; Update a user
(defn update-user [id user]
  (log/info "Updating user with ID:" id "Data:" user)
  (if-let [existing-user (user-exists? id)]
    (let [result (model/update-user id user)]
      (if (= 1 (:update-count result))
        (success 200 "User updated successfully")
        (error 400 "Failed to update user")))
    existing-user))

;; Update a user's username
(defn update-user-username [id new-username]
  (log/info "Updating username for user ID:" id)
  (if-let [user (user-exists? id)]
    (let [result (model/update-user id {:username new-username})]
      (if (= 1 (:update-count result))
        (success 200 "Username updated successfully")
        (error 400 "Failed to update username")))
    user))

;; Update a user's email
(defn update-user-email [id new-email]
  (log/info "Updating email for user ID:" id)
  (if-let [user (user-exists? id)]
    (let [result (model/update-user id {:email new-email})]
      (if (= 1 (:update-count result))
        (success 200 "Email updated successfully")
        (error 400 "Failed to update email")))
    user))

;; Update a user's password
(defn update-user-password [id new-password]
  (log/info "Updating password for user ID:" id)
  (if-let [user (user-exists? id)]
    (let [result (model/update-user-password id (hashers/derive new-password))]
      (success 200 "Password updated successfully"))
    user))

;; Delete a user
(defn delete-user [id]
  (log/info "Deleting user with ID:" id)
  (if-let [user (user-exists? id)]
    (let [result (model/delete-user id)]
      (if (= 1 (:delete-count result))
        (success 200 "User deleted successfully")
        (error 400 "Failed to delete user")))
    user))

;; Get roles for a user
(defn get-roles-for-user [id]
  (log/info "Fetching roles for user ID:" id)
  (if-let [user (user-exists? id)]
    (work 200 (model/get-roles-for-user id))
    user))

;; Add roles to a user
(defn add-roles-to-user [id roles]
  (log/info "Adding roles to user ID:" id "Roles:" roles)
  (if-let [user (user-exists? id)]
    (let [results (map #(role-model/add-role-to-user % id) roles)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count roles) success-count)]
      (success 200 {:success-count success-count :failure-count failure-count}))
    user))

;; Remove roles from a user
(defn remove-roles-from-user [id roles]
  (log/info "Removing roles from user ID:" id "Roles:" roles)
  (if-let [user (user-exists? id)]
    (let [results (map #(role-model/remove-role-from-user id %) roles)
          success-count (count (filter #(= 1 (:update-count %)) results))
          failure-count (- (count roles) success-count)]
      (success 200 {:success-count success-count :failure-count failure-count}))
    user))

;; Get permissions for a user
(defn get-permissions-for-user [id]
  (log/info "Fetching permissions for user ID:" id)
  (if-let [user (user-exists? id)]
    (work 200 (model/get-permissions-for-user id))
    user))