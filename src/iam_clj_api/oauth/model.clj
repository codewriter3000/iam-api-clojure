(ns iam-clj-api.oauth.model
  (:require [next.jdbc :as jdbc]
            [lib.core :refer :all]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(def ds (get-datasource))

(defn- as-int [value]
  (try
    (if (integer? value)
      value
      (Integer/parseInt (str value)))
    (catch Exception _
      nil)))

(defn- log-query [query params]
  (log/info "Executing query:" query "with params:" params))

(defn create-oauth-table []
  (jdbc/execute! ds
                 ["CREATE TABLE IF NOT EXISTS oauth_clients (
                    id SERIAL PRIMARY KEY,
                    client_id VARCHAR(128) NOT NULL UNIQUE,
                    client_name VARCHAR(256) NOT NULL,
                    client_secret_hash VARCHAR(256),
                    token_endpoint_auth_method VARCHAR(64) NOT NULL DEFAULT 'client_secret_basic',
                    is_confidential BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  );

                  CREATE TABLE IF NOT EXISTS oauth_client_redirect_uris (
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    redirect_uri VARCHAR(2000) NOT NULL,
                    PRIMARY KEY (client_id, redirect_uri)
                  );

                  CREATE TABLE IF NOT EXISTS oauth_client_grants (
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    grant_type VARCHAR(64) NOT NULL,
                    PRIMARY KEY (client_id, grant_type)
                  );

                  CREATE TABLE IF NOT EXISTS oauth_scopes (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(128) NOT NULL UNIQUE,
                    description VARCHAR(1000)
                  );

                  CREATE TABLE IF NOT EXISTS oauth_client_scopes (
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    scope_id INT NOT NULL REFERENCES oauth_scopes(id) ON DELETE CASCADE,
                    PRIMARY KEY (client_id, scope_id)
                  );

                  CREATE TABLE IF NOT EXISTS oauth_authorization_codes (
                    id SERIAL PRIMARY KEY,
                    code_hash VARCHAR(128) NOT NULL UNIQUE,
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    redirect_uri VARCHAR(2000) NOT NULL,
                    scope VARCHAR(1000),
                    expires_at TIMESTAMP NOT NULL,
                    consumed_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  );

                  CREATE TABLE IF NOT EXISTS oauth_access_tokens (
                    id SERIAL PRIMARY KEY,
                    token_hash VARCHAR(128) NOT NULL UNIQUE,
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    user_id INT REFERENCES users(id) ON DELETE CASCADE,
                    grant_type VARCHAR(64) NOT NULL,
                    scope VARCHAR(1000),
                    token_type VARCHAR(20) NOT NULL DEFAULT 'Bearer',
                    expires_at TIMESTAMP NOT NULL,
                    revoked_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  );

                  CREATE TABLE IF NOT EXISTS oauth_refresh_tokens (
                    id SERIAL PRIMARY KEY,
                    token_hash VARCHAR(128) NOT NULL UNIQUE,
                    access_token_id INT REFERENCES oauth_access_tokens(id) ON DELETE CASCADE,
                    client_id INT NOT NULL REFERENCES oauth_clients(id) ON DELETE CASCADE,
                    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    scope VARCHAR(1000),
                    expires_at TIMESTAMP NOT NULL,
                    revoked_at TIMESTAMP,
                    replaced_by_token_id INT REFERENCES oauth_refresh_tokens(id),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  );"] ))

(defn drop-oauth-table []
  (jdbc/execute! ds
                 ["DROP TABLE IF EXISTS oauth_refresh_tokens;
                   DROP TABLE IF EXISTS oauth_access_tokens;
                   DROP TABLE IF EXISTS oauth_authorization_codes;
                   DROP TABLE IF EXISTS oauth_client_scopes;
                   DROP TABLE IF EXISTS oauth_scopes;
                   DROP TABLE IF EXISTS oauth_client_grants;
                   DROP TABLE IF EXISTS oauth_client_redirect_uris;
                   DROP TABLE IF EXISTS oauth_clients;"]))

(defn list-scopes []
  (let [query "SELECT id, name, description FROM oauth_scopes ORDER BY name;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
    (map remove-namespace result)))

(defn get-scope-by-name [scope-name]
  (let [query "SELECT id, name, description FROM oauth_scopes WHERE name = ?;"
        params [scope-name]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn create-scope [scope]
  (let [query "INSERT INTO oauth_scopes (name, description) VALUES (?, ?) RETURNING id, name, description;"
        params [(:name scope) (:description scope)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn delete-scope [scope-name]
  (let [query "DELETE FROM oauth_scopes WHERE name = ?;"
        params [scope-name]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn list-clients []
  (let [query "SELECT id, client_id, client_name, token_endpoint_auth_method, is_confidential, created_at, updated_at FROM oauth_clients ORDER BY client_id;"
        result (jdbc/execute! ds [query])]
    (log-query query [])
    (map remove-namespace result)))

(defn get-client-by-id [id]
  (let [query "SELECT id, client_id, client_name, client_secret_hash, token_endpoint_auth_method, is_confidential, created_at, updated_at FROM oauth_clients WHERE id = ?;"
        params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-client-by-client-id [client-id]
  (let [query "SELECT id, client_id, client_name, client_secret_hash, token_endpoint_auth_method, is_confidential, created_at, updated_at FROM oauth_clients WHERE client_id = ?;"
        params [client-id]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-client-redirect-uris [client-id]
  (let [query "SELECT redirect_uri FROM oauth_client_redirect_uris WHERE client_id = ? ORDER BY redirect_uri;"
        params [(as-int client-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map :redirect_uri (map remove-namespace result))))

(defn get-client-grants [client-id]
  (let [query "SELECT grant_type FROM oauth_client_grants WHERE client_id = ? ORDER BY grant_type;"
        params [(as-int client-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map :grant_type (map remove-namespace result))))

(defn get-client-scopes [client-id]
  (let [query "SELECT s.name
               FROM oauth_scopes s
               JOIN oauth_client_scopes cs ON s.id = cs.scope_id
               WHERE cs.client_id = ?
               ORDER BY s.name;"
        params [(as-int client-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (map :name (map remove-namespace result))))

(defn- clear-client-redirect-uris! [tx client-id]
  (jdbc/execute! tx ["DELETE FROM oauth_client_redirect_uris WHERE client_id = ?;" (as-int client-id)]))

(defn- clear-client-grants! [tx client-id]
  (jdbc/execute! tx ["DELETE FROM oauth_client_grants WHERE client_id = ?;" (as-int client-id)]))

(defn- clear-client-scopes! [tx client-id]
  (jdbc/execute! tx ["DELETE FROM oauth_client_scopes WHERE client_id = ?;" (as-int client-id)]))

(defn- set-client-redirect-uris! [tx client-id redirect-uris]
  (doseq [redirect-uri redirect-uris]
    (jdbc/execute! tx ["INSERT INTO oauth_client_redirect_uris (client_id, redirect_uri) VALUES (?, ?);"
                       (as-int client-id) redirect-uri])))

(defn- set-client-grants! [tx client-id grants]
  (doseq [grant-type grants]
    (jdbc/execute! tx ["INSERT INTO oauth_client_grants (client_id, grant_type) VALUES (?, ?);"
                       (as-int client-id) grant-type])))

(defn- set-client-scopes! [tx client-id scopes]
  (doseq [scope-name scopes]
    (when-let [scope (get-scope-by-name scope-name)]
      (jdbc/execute! tx ["INSERT INTO oauth_client_scopes (client_id, scope_id) VALUES (?, ?);"
                         (as-int client-id) (:id scope)]))))

(defn create-client [client]
  (jdbc/with-transaction [tx ds]
    (let [query "INSERT INTO oauth_clients (client_id, client_name, client_secret_hash, token_endpoint_auth_method, is_confidential)
                 VALUES (?, ?, ?, ?, ?) RETURNING id, client_id, client_name, token_endpoint_auth_method, is_confidential, created_at, updated_at;"
          params [(:client_id client)
                  (:client_name client)
                  (:client_secret_hash client)
                  (:token_endpoint_auth_method client)
                  (:is_confidential client)]
          created-client (some-> (jdbc/execute! tx (into [query] params)) first remove-namespace)]
      (log-query query params)
      (set-client-redirect-uris! tx (:id created-client) (:redirect_uris client))
      (set-client-grants! tx (:id created-client) (:grants client))
      (set-client-scopes! tx (:id created-client) (:scopes client))
      (assoc created-client
             :redirect_uris (:redirect_uris client)
             :grants (:grants client)
             :scopes (:scopes client)))))

(defn update-client [id client]
  (jdbc/with-transaction [tx ds]
    (let [query "UPDATE oauth_clients
                 SET client_name = ?, token_endpoint_auth_method = ?, is_confidential = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?;"
          params [(:client_name client)
                  (:token_endpoint_auth_method client)
                  (:is_confidential client)
                  (as-int id)]
          _ (log-query query params)
          result (jdbc/execute! tx (into [query] params))
          update-count (:next.jdbc/update-count (first result))]
      (when (= 1 update-count)
        (clear-client-redirect-uris! tx id)
        (clear-client-grants! tx id)
        (clear-client-scopes! tx id)
        (set-client-redirect-uris! tx id (:redirect_uris client))
        (set-client-grants! tx id (:grants client))
        (set-client-scopes! tx id (:scopes client)))
      update-count)))

(defn update-client-secret [id secret-hash]
  (let [query "UPDATE oauth_clients SET client_secret_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?;"
        params [secret-hash (as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn delete-client [id]
  (let [query "DELETE FROM oauth_clients WHERE id = ?;"
        params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn create-auth-code [auth-code]
  (let [query "INSERT INTO oauth_authorization_codes (code_hash, client_id, user_id, redirect_uri, scope, expires_at)
               VALUES (?, ?, ?, ?, ?, ?) RETURNING id, code_hash, client_id, user_id, redirect_uri, scope, expires_at, consumed_at, created_at;"
        params [(:code_hash auth-code)
                (as-int (:client_id auth-code))
                (as-int (:user_id auth-code))
                (:redirect_uri auth-code)
                (:scope auth-code)
                (:expires_at auth-code)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-active-auth-code [code-hash]
  (let [query "SELECT id, code_hash, client_id, user_id, redirect_uri, scope, expires_at, consumed_at, created_at
               FROM oauth_authorization_codes
               WHERE code_hash = ?
                 AND consumed_at IS NULL
                 AND expires_at > CURRENT_TIMESTAMP;"
        params [code-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn consume-auth-code [id]
  (let [query "UPDATE oauth_authorization_codes SET consumed_at = CURRENT_TIMESTAMP WHERE id = ?;"
        params [(as-int id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn create-access-token [token]
  (let [query "INSERT INTO oauth_access_tokens (token_hash, client_id, user_id, grant_type, scope, token_type, expires_at)
               VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id, token_hash, client_id, user_id, grant_type, scope, token_type, expires_at, revoked_at, created_at;"
        params [(:token_hash token)
                (as-int (:client_id token))
                (some-> (:user_id token) as-int)
                (:grant_type token)
                (:scope token)
                (:token_type token)
                (:expires_at token)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn create-refresh-token [token]
  (let [query "INSERT INTO oauth_refresh_tokens (token_hash, access_token_id, client_id, user_id, scope, expires_at)
               VALUES (?, ?, ?, ?, ?, ?) RETURNING id, token_hash, access_token_id, client_id, user_id, scope, expires_at, revoked_at, replaced_by_token_id, created_at;"
        params [(:token_hash token)
                (as-int (:access_token_id token))
                (as-int (:client_id token))
                (as-int (:user_id token))
                (:scope token)
                (:expires_at token)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-active-access-token [token-hash]
  (let [query "SELECT id, token_hash, client_id, user_id, grant_type, scope, token_type, expires_at, revoked_at, created_at
               FROM oauth_access_tokens
               WHERE token_hash = ?
                 AND revoked_at IS NULL
                 AND expires_at > CURRENT_TIMESTAMP;"
        params [token-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-active-refresh-token [token-hash]
  (let [query "SELECT id, token_hash, access_token_id, client_id, user_id, scope, expires_at, revoked_at, replaced_by_token_id, created_at
               FROM oauth_refresh_tokens
               WHERE token_hash = ?
                 AND revoked_at IS NULL
                 AND expires_at > CURRENT_TIMESTAMP;"
        params [token-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn revoke-access-token [token-id]
  (let [query "UPDATE oauth_access_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE id = ? AND revoked_at IS NULL;"
        params [(as-int token-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn revoke-access-token-by-hash [token-hash client-id]
  (let [query "UPDATE oauth_access_tokens
               SET revoked_at = CURRENT_TIMESTAMP
               WHERE token_hash = ?
                 AND client_id = ?
                 AND revoked_at IS NULL;"
        params [token-hash (as-int client-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn revoke-refresh-token [token-id]
  (let [query "UPDATE oauth_refresh_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE id = ? AND revoked_at IS NULL;"
        params [(as-int token-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn revoke-refresh-token-by-hash [token-hash client-id]
  (let [query "UPDATE oauth_refresh_tokens
               SET revoked_at = CURRENT_TIMESTAMP
               WHERE token_hash = ?
                 AND client_id = ?
                 AND revoked_at IS NULL;"
        params [token-hash (as-int client-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn mark-refresh-replaced [old-id new-id]
  (let [query "UPDATE oauth_refresh_tokens SET replaced_by_token_id = ? WHERE id = ?;"
        params [(as-int new-id) (as-int old-id)]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (:next.jdbc/update-count (first result))))

(defn get-access-token-by-hash [token-hash]
  (let [query "SELECT id, token_hash, client_id, user_id, grant_type, scope, token_type, expires_at, revoked_at, created_at
               FROM oauth_access_tokens
               WHERE token_hash = ?;"
        params [token-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn get-refresh-token-by-hash [token-hash]
  (let [query "SELECT id, token_hash, access_token_id, client_id, user_id, scope, expires_at, revoked_at, replaced_by_token_id, created_at
               FROM oauth_refresh_tokens
               WHERE token_hash = ?;"
        params [token-hash]
        result (jdbc/execute! ds (into [query] params))]
    (log-query query params)
    (some-> result first remove-namespace)))

(defn scope-string [scope-list]
  (->> scope-list
       (remove str/blank?)
       distinct
       sort
       (str/join " ")))

(defn seed-default-oauth-scopes []
  (doseq [scope [{:name "openid" :description "OpenID Connect authentication scope"}
                 {:name "profile" :description "Profile scope"}
                 {:name "email" :description "Email scope"}
                 {:name "read" :description "Read access"}
                 {:name "write" :description "Write access"}
                 {:name "admin" :description "Administrative access"}]]
    (when-not (get-scope-by-name (:name scope))
      (create-scope scope))))
