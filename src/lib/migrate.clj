(ns lib.migrate)

;Create tables from iam-clj-api.user.model.create-user-table, iam-clj-api.role.model.create-role-table, and iam-clj-api.permission.model.create-permission-table.

(require '[iam-clj-api.user.model :as user-model])
(require '[iam-clj-api.role.model :as role-model])
(require '[iam-clj-api.permission.model :as permission-model])

(defn create-tables []
  (user-model/create-user-table)
  (role-model/create-role-table)
  (permission-model/create-permission-table))

;Drop tables from iam-clj-api.user.model.drop-user-table, iam-clj-api.role.model.drop-role-table, and iam-clj-api.permission.model.drop-permission-table.

(defn drop-tables []
  (user-model/drop-user-table)
  (role-model/drop-role-table)
  (permission-model/drop-permission-table))

;Create tables and drop tables from the command line.

(defn add-core-perms-and-users []
  (permission-model/insert-permission {:name "Administrator" :description "Grants full system access, including user management, role assignment, permission configuration, and all administrative operations."})
  (permission-model/insert-permission {:name "Active" :description "Indicates that the user account is enabled and permitted to authenticate and access the system."})
  (user-model/insert-user {:username "root" :email "root@example.com" :first_name "Root" :last_name "User" :password "changeme"})
  (permission-model/add-permission-to-user
    (:id (permission-model/get-permission-by-name "Administrator"))
    (:id (user-model/get-user-by-username "root"))))

(defn -main [& args]
  (case (first args)
    "init" (do
             (create-tables)
             (add-core-perms-and-users))
    "create-tables" (create-tables)
    "drop-tables" (drop-tables)
    "add-core-perms-and-users" (add-core-perms-and-users)
    (println "Invalid command")))

;Run the following commands from the command line to create and drop tables.

;Run both create tables and add admin and root accounts
;lein run -m lib.migrate init

;Create tables
;lein run -m lib.migrate create-tables

;Drop tables
;lein run -m lib.migrate drop-tables

;Add core perms and users
;lein run -m lib.migrate add-core-perms-and-users
