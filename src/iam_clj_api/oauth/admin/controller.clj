(ns iam-clj-api.oauth.admin.controller
  (:require [clojure.string :as str]
            [iam-clj-api.oauth.admin.model :as model]
            [iam-clj-api.oauth.crypto :as crypto]
            [iam-clj-api.user.model :as user-model]
            [lib.response :refer [error success work]]))

(defn- administrator? [request]
  (let [user-id (get-in request [:session :user-id])
        permissions (user-model/get-permissions-for-user user-id)]
    (boolean (some #(= "Administrator" (:name %)) permissions))))

(defn- require-admin [request]
  (when-not (administrator? request)
    (error 403 "Administrator access required")))

(defn list-scopes [request]
  (if-let [admin-error (require-admin request)]
    admin-error
    (work 200 {:scopes (model/list-scopes)})))

(defn create-scope [request payload]
  (if-let [admin-error (require-admin request)]
    admin-error
    (if (str/blank? (:name payload))
      (error 400 "Scope name is required")
      (if (model/get-scope-by-name (:name payload))
        (error 400 "Scope already exists")
        (work 201 {:scope (model/create-scope payload)})))))

(defn delete-scope [request scope-name]
  (if-let [admin-error (require-admin request)]
    admin-error
    (if (= 1 (model/delete-scope scope-name))
      (success 204 "Scope deleted")
      (error 404 "Scope not found"))))

(defn- hydrate-client [client]
  (when client
    (-> client
        (dissoc :client_secret_hash)
        (assoc :redirect_uris (model/get-client-redirect-uris (:id client))
               :grants (model/get-client-grants (:id client))
               :scopes (model/get-client-scopes (:id client))))))

(defn list-clients [request]
  (if-let [admin-error (require-admin request)]
    admin-error
    (work 200 {:clients (map hydrate-client (model/list-clients))})))

(defn get-client [request id]
  (if-let [admin-error (require-admin request)]
    admin-error
    (if-let [client (hydrate-client (model/get-client-by-id id))]
      (work 200 {:client client})
      (error 404 "Client not found"))))

(defn create-client [request payload]
  (if-let [admin-error (require-admin request)]
    admin-error
    (let [existing-client (model/get-client-by-client-id (:client_id payload))
          is-confidential (if (contains? payload :is_confidential) (:is_confidential payload) true)
          auth-method (or (:token_endpoint_auth_method payload) "client_secret_basic")
          raw-secret (or (:client_secret payload) (when is-confidential (crypto/random-token 24)))
          secret-hash (when raw-secret (crypto/derive-secret raw-secret))
          grants (or (:grants payload) ["authorization_code" "refresh_token"])
          scopes (or (:scopes payload) ["openid" "profile" "email"])
          redirect-uris (or (:redirect_uris payload) [])]
      (cond
        (str/blank? (:client_id payload)) (error 400 "client_id is required")
        (str/blank? (:client_name payload)) (error 400 "client_name is required")
        (seq existing-client) (error 400 "client_id already exists")
        (and is-confidential (str/blank? raw-secret)) (error 400 "client_secret is required for confidential clients")
        :else
        (let [created-client (model/create-client {:client_id (:client_id payload)
                                                   :client_name (:client_name payload)
                                                   :client_secret_hash secret-hash
                                                   :token_endpoint_auth_method auth-method
                                                   :is_confidential is-confidential
                                                   :redirect_uris redirect-uris
                                                   :grants grants
                                                   :scopes scopes})]
          (work 201 (cond-> {:client created-client}
                      (seq raw-secret) (assoc :client_secret raw-secret))))))))

(defn update-client [request id payload]
  (if-let [admin-error (require-admin request)]
    admin-error
    (if-let [existing (model/get-client-by-id id)]
      (let [is-confidential (if (contains? payload :is_confidential)
                              (:is_confidential payload)
                              (:is_confidential existing))
            update-count (model/update-client id
                                              {:client_name (or (:client_name payload) (:client_name existing))
                                               :token_endpoint_auth_method (or (:token_endpoint_auth_method payload)
                                                                               (:token_endpoint_auth_method existing))
                                               :is_confidential is-confidential
                                               :redirect_uris (or (:redirect_uris payload)
                                                                  (model/get-client-redirect-uris id))
                                               :grants (or (:grants payload)
                                                           (model/get-client-grants id))
                                               :scopes (or (:scopes payload)
                                                           (model/get-client-scopes id))})
            raw-secret (:client_secret payload)
            _ (when (and raw-secret (= 1 update-count))
                (model/update-client-secret id (crypto/derive-secret raw-secret)))]
        (if (= 1 update-count)
          (work 200 (cond-> {:client (hydrate-client (model/get-client-by-id id))}
                      (seq raw-secret) (assoc :client_secret raw-secret)))
          (error 500 "Failed to update client")))
      (error 404 "Client not found"))))

(defn delete-client [request id]
  (if-let [admin-error (require-admin request)]
    admin-error
    (if (= 1 (model/delete-client id))
      (success 204 "Client deleted")
      (error 404 "Client not found"))))
