(ns lib.exists
  (:require
   [clojure.tools.logging :as log]
   [iam-clj-api.user.model :as user-model]
   [iam-clj-api.role.model :as role-model]
   [iam-clj-api.permission.model :as permission-model]))

;; Helper function to check if a user exists
(defn user-exists? [id]
  (let [user (user-model/get-user-by-id id)]
    (if user
      user
      nil)))

;; Helper function to check if a role exists
(defn role-exists? [id]
  (let [role (role-model/get-role-by-id id)]
    (if role
      role
      nil)))

;; Helper function to check if a permission exists
(defn permission-exists? [id]
  (let [permission (permission-model/get-permission-by-id id)]
    (if permission
      permission
      nil)))