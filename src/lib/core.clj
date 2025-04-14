(ns lib.core
  (:require
   [env :as env]
   [next.jdbc :as jdbc]))

(defn remove-namespace [m]
  (into {} (map (fn [[k v]] [(keyword (name k)) v]) m)))

(defn get-datasource []
  (jdbc/get-datasource {:dbtype (get env/_ :DATABASE_TYPE)
                        :dbname (get env/_ :DATABASE_NAME)
                        :user (get env/_ :DATABASE_USER)
                        :host (get env/_ :DATABASE_HOST)
                        :port (get env/_ :DATABASE_PORT)
                        :password (get env/_ :DATABASE_PASS)}))