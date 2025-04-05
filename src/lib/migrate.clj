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

(defn -main [& args]
  (case (first args)
    "create-tables" (create-tables)
    "drop-tables" (drop-tables)
    (println "Invalid command")))

;Run the following commands from the command line to create and drop tables.

;Create tables
;lein run -m lib.migrate create-tables

;Drop tables
;lein run -m lib.migrate drop-tables

