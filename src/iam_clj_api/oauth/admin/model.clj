(ns iam-clj-api.oauth.admin.model
  (:require [iam-clj-api.oauth.model :as oauth-model]))

(defn list-clients []
  (oauth-model/list-clients))

(defn get-client-by-id [id]
  (oauth-model/get-client-by-id id))

(defn get-client-by-client-id [client-id]
  (oauth-model/get-client-by-client-id client-id))

(defn create-client [client]
  (oauth-model/create-client client))

(defn update-client [id client]
  (oauth-model/update-client id client))

(defn update-client-secret [id secret-hash]
  (oauth-model/update-client-secret id secret-hash))

(defn delete-client [id]
  (oauth-model/delete-client id))

(defn get-client-redirect-uris [client-id]
  (oauth-model/get-client-redirect-uris client-id))

(defn get-client-grants [client-id]
  (oauth-model/get-client-grants client-id))

(defn get-client-scopes [client-id]
  (oauth-model/get-client-scopes client-id))

(defn list-scopes []
  (oauth-model/list-scopes))

(defn get-scope-by-name [scope-name]
  (oauth-model/get-scope-by-name scope-name))

(defn create-scope [scope]
  (oauth-model/create-scope scope))

(defn delete-scope [scope-name]
  (oauth-model/delete-scope scope-name))
