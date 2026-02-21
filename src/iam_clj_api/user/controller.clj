(ns iam-clj-api.user.controller
  (:require [lib.core :refer :all]
            [iam-clj-api.user.model :as model]
            [iam-clj-api.role.model :as role-model]
            [buddy.hashers :as hashers]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lib.response :refer [error success work]]
            [lib.exists :refer [user-exists?]]))

(defn- sha256 [value]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes value "UTF-8"))]
    (format "%064x" (java.math.BigInteger. 1 digest))))

(defn- now-plus-minutes [minutes]
  (java.sql.Timestamp. (+ (System/currentTimeMillis) (* minutes 60 1000))))

(defn- frontend-base-url []
  (or (System/getenv "FRONTEND_BASE_URL")
      "http://localhost:3000"))

(defn- build-reset-url [token]
  (str (frontend-base-url) "/reset-password?token=" token))

(defn- reset-generic-response []
  (success 200 "If the account exists, a password reset link has been sent"))

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

(defn- enabled-account? [permission-names]
  (let [normalized (->> permission-names
                        (map #(-> % str str/trim str/lower-case))
                        (remove str/blank?)
                        set)]
    (or (contains? normalized "active")
        (contains? normalized "enabled"))))

(defn- normalize-username [username]
  (-> username
      (or "")
      str
      str/trim
      str/lower-case))

(defn- root-user? [user]
  (= "root" (normalize-username (:username user))))

(defn- build-user-login-payload [user]
  (let [permissions (->> (model/get-permissions-for-user (:id user))
                         (map :name)
                         (remove str/blank?)
                         distinct
                         vec)]
    {:id (:id user)
     :username (:username user)
     :email (:email user)
     :first_name (:first_name user)
     :last_name (:last_name user)
     :permissions permissions}))

(defn password-reset-required? [id]
  (if-let [user (user-exists? id)]
    (boolean (:force_password_reset user))
    false))

(defn- username-change-attempt? [existing-user payload]
  (and (contains? payload :username)
       (some? (:username payload))
       (not= (normalize-username (:username existing-user))
             (normalize-username (:username payload)))))

;; Insert a new user
(defn insert-user [user]
  (log/info "Inserting user:" user)
  (let [validated-input (validate-input user)]
    (if (:error validated-input)
      (error 422 (:error validated-input)) ; Pass the error message directly
      (do
        (model/insert-user validated-input)
        (success 201 "User created successfully"))))) ; Pass the success message directly as a string

;; Login a user
(defn login-user [login-id password]
  (log/info "Logging in user:" login-id)
  (let [user (model/get-user-by-login-id login-id)]
    (if (and user (hashers/check password (get user :password)))
      (if (boolean (:force_password_reset user))
        (work 200 {:message "Password reset required"
                   :user {:id (:id user)
                          :username (:username user)
                          :email (:email user)
                          :first_name (:first_name user)
                          :last_name (:last_name user)
                          :permissions []}})
        (let [login-user (build-user-login-payload user)]
          (if (enabled-account? (:permissions login-user))
            (work 200 {:message "Login successful"
                       :user login-user})
            (error 403 "Account is disabled"))))
      (error 401 "Invalid username or password"))))

(defn request-password-reset [login-id]
  (log/info "Password reset requested for login-id:" login-id)
  (if (str/blank? login-id)
    (do
      (log/warn "Password reset skipped: blank login-id")
      (reset-generic-response))
    (if-let [user (model/get-user-by-login-id login-id)]
      (let [token (str (java.util.UUID/randomUUID))
            token-hash (sha256 token)
            expires-at (now-plus-minutes 15)
            reset-url (build-reset-url token)]
        (model/set-password-reset-token (:id user) token-hash expires-at)
        (log/info "Generated password reset URL:" reset-url)
        (reset-generic-response))
      (do
        (log/warn "Password reset skipped: no user found for login-id" login-id)
        (reset-generic-response)))))

(defn reset-password [token new-password session-user-id force-reset-authorized]
  (log/info "Resetting password with token")
  (if (str/blank? new-password)
    (error 400 "Password is required")
    (if (str/blank? token)
      (if (and session-user-id
               force-reset-authorized
               (password-reset-required? session-user-id))
        (let [result (model/update-user-password session-user-id (hashers/derive new-password))]
          (if (= 1 (:next.jdbc/update-count (first result)))
            (do
              (model/set-force-password-reset session-user-id false)
              (model/consume-password-reset-token session-user-id)
              (if-let [updated-user (model/get-user-by-id session-user-id)]
                (work 200 {:message "Password reset successfully"
                           :user (build-user-login-payload updated-user)})
                (error 500 "Failed to load user after password reset")))
            (error 500 "Failed to reset password")))
        (error 403 "Password reset requires a verified login or valid reset token"))
      (if-let [user (model/get-user-by-reset-token-hash (sha256 token))]
        (let [result (model/update-user-password (:id user) (hashers/derive new-password))]
          (if (= 1 (:next.jdbc/update-count (first result)))
            (do
              (model/set-force-password-reset (:id user) false)
              (model/consume-password-reset-token (:id user))
              (if-let [updated-user (model/get-user-by-id (:id user))]
                (work 200 {:message "Password reset successfully"
                           :user (build-user-login-payload updated-user)})
                (error 500 "Failed to load user after password reset")))
            (error 500 "Failed to reset password")))
        (error 400 "Invalid or expired reset token")))))

(defn get-session-user [id]
  (if-let [user (user-exists? id)]
    (if (boolean (:force_password_reset user))
      (error 403 "Password reset required before access is allowed")
      (let [permissions (->> (model/get-permissions-for-user id)
                             (map :name)
                             (remove str/blank?)
                             distinct
                             vec)]
      (work 200 {:message "Session valid"
                 :user {:id (:id user)
                        :username (:username user)
                        :email (:email user)
                        :first_name (:first_name user)
                        :last_name (:last_name user)
                        :permissions permissions}})))
    (error 401 "Invalid session")))

(defn force-password-reset [id]
  (log/info "Forcing password reset for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/set-force-password-reset id true)]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Password reset will be required on next sign in")
          (error 500 "Failed to force password reset")))
      (error 404 "User not found"))))

;; Get all users
(defn get-all-users []
  (log/info "Fetching all users")
  (let [users (map remove-namespace (model/get-all-users))
        result (work 200 {:users users})]
    (log/info "Response: " result)
    result))

;; Get a user by ID
(defn get-user-by-id [id]
  (log/info "Fetching user by ID:" id)
  (let [user (user-exists? id)]
    (if user
      (work 200 {:user user})
      (error 404 "User not found"))))

;; Update a user
(defn update-user [id user]
  (log/info "Updating user with ID:" id "Data:" user)
  (let [existing-user (user-exists? id)]
    (log/info "User exists:" existing-user)
    (if existing-user
      (if (and (root-user? existing-user)
               (username-change-attempt? existing-user user))
        (error 403 "Cannot change root username")
        (do
          (log/info "User found:" existing-user)
          (let [result (model/update-user id user)]
            (log/info "Update result:" (:next.jdbc/update-count (first result)))
            (if (= 1 (:next.jdbc/update-count (first result)))
              (let [updated-user (merge existing-user user)] ; Merge existing and updated fields
                (log/info "Updated user:" updated-user)
                (work 200 {:user updated-user}))
              (error 500 "Failed to update user")))))
      (error 404 "User not found"))))

;; Update a user's username
(defn update-user-username [id new-username]
  (log/info "Updating username for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (if (and (root-user? user)
               (not= (normalize-username (:username user))
                     (normalize-username new-username)))
        (error 403 "Cannot change root username")
        (let [result (model/update-user id {:username new-username})]
          (if (= 1 (:next.jdbc/update-count (first result)))
            (success 200 "Username updated successfully")
            (error 500 "Failed to update username"))))
      (error 404 "User not found"))))

;; Update a user's email
(defn update-user-email [id new-email]
  (log/info "Updating email for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/update-user id {:email new-email})]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Email updated successfully")
          (error 500 "Failed to update email")))
      (error 404 "User not found"))))

;; Update a user's password
(defn update-user-password [id new-password]
  (log/info "Updating password for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (let [result (model/update-user-password id (hashers/derive new-password))]
        (if (= 1 (:next.jdbc/update-count (first result)))
          (success 200 "Password updated successfully")
          (error 500 "Failed to update password")))
      (error 404 "User not found"))))

;; Delete a user
(defn delete-user [id]
  (log/info "Deleting user with ID:" id)
  (let [user (user-exists? id)]
    (if user
      (if (root-user? user)
        (error 403 "Cannot delete root user")
        (let [result (model/delete-user id)]
          (if result
            (success 204 "User deleted successfully")
            (error 500 "Failed to delete user"))))
      (error 404 "User not found"))))

;; Get roles for a user
(defn get-roles-for-user [id]
  (log/info "Fetching roles for user ID:" id)
  (let [user (user-exists? id)]
    (if user
      (work 200 {:roles (model/get-roles-for-user id)})
      (error 404 "User not found"))))

;; Add roles to a user
(defn add-roles-to-user [id roles]
  (log/info "Adding roles to user ID:" id "Roles:" roles)
  (if (empty? roles)
    (do
      (log/info "No roles provided")
      (error 400 "No roles provided"))
    (do
      (log/info "Roles provided:" roles)
      (let [user (user-exists? id)]
        (if user
          (let [results (map #(role-model/add-role-to-user % id) roles)
                success-count (count (filter #(= 1 (:update-count %)) results))
                failure-count (- (count roles) success-count)]
            (success 200 {:success-count success-count :failure-count failure-count}))
          (error 404 "User not found"))))))

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
      (work 200 {:permissions (model/get-permissions-for-user id)})
      (error 404 "User not found"))))