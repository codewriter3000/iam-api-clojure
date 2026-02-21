(ns iam-clj-api.db.migrate
  (:require [buddy.hashers :as hashers]
            [iam-clj-api.oauth.model :as oauth-model]
            [iam-clj-api.permission.model :as permission-model]
            [iam-clj-api.role.model :as role-model]
            [iam-clj-api.user.model :as user-model]))

(defn create-tables []
  (user-model/create-user-table)
  (role-model/create-role-table)
  (permission-model/create-permission-table)
  (oauth-model/create-oauth-table))

(defn drop-tables []
  (oauth-model/drop-oauth-table)
  (user-model/drop-user-table)
  (role-model/drop-role-table)
  (permission-model/drop-permission-table))

(defn add-core-perms-and-users []
  (permission-model/insert-permission {:name "Administrator" :description "Grants full system access, including user management, role assignment, permission configuration, and all administrative operations."})
  (permission-model/insert-permission {:name "Active" :description "Indicates that the user account is enabled and permitted to authenticate and access the system."})
  (user-model/insert-user {:username "root" :email "root@example.com" :first_name "Root" :last_name "User" :password (hashers/derive "changeme")})
  (permission-model/add-permission-to-user
    (:id (permission-model/get-permission-by-name "Administrator"))
    (:id (user-model/get-user-by-username "root")))
  (permission-model/add-permission-to-user
    (:id (permission-model/get-permission-by-name "Active"))
    (:id (user-model/get-user-by-username "root"))))

(defn add-oauth-defaults []
  (oauth-model/seed-default-oauth-scopes))

(defn -main [& args]
  (case (first args)
    "init" (do
             (create-tables)
             (add-core-perms-and-users)
             (add-oauth-defaults))
    "create-tables" (create-tables)
    "drop-tables" (drop-tables)
    "add-core-perms-and-users" (add-core-perms-and-users)
    "add-oauth-defaults" (add-oauth-defaults)
    (println "Invalid command")))

; Run both create tables and add admin and root accounts
; lein run -m iam-clj-api.db.migrate init

; Create tables
; lein run -m iam-clj-api.db.migrate create-tables

; Drop tables
; lein run -m iam-clj-api.db.migrate drop-tables

; Add core perms and users
; lein run -m iam-clj-api.db.migrate add-core-perms-and-users

; Add OAuth defaults
; lein run -m iam-clj-api.db.migrate add-oauth-defaults
