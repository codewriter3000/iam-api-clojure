(ns iam-clj-api.schemas
  (:require [schema.core :as s]))

(s/defschema User
  {:id Integer
   :username String
   :email String
   (s/optional-key :first_name) (s/maybe String)
   (s/optional-key :last_name) (s/maybe String)
   (s/optional-key :force_password_reset) Boolean
   (s/optional-key :created_at) s/Inst})

(s/defschema Role
  {:id Integer
   :name String
   :description (s/maybe String)})

(s/defschema Permission
  {:id Integer
   :name String
   :description (s/maybe String)})

(s/defschema UserWithRoles
  (assoc User :roles [Role]))

(s/defschema RoleWithUsers
  (assoc Role :users [User]))

(s/defschema UsersResponse {:users [User]})
(s/defschema UserResponse {:user User})
(s/defschema UserWithRolesResponse {:user UserWithRoles})

(s/defschema RolesResponse {:roles [Role]})
(s/defschema RoleResponse {:role Role})
(s/defschema RoleWithUsersResponse {:role RoleWithUsers})

(s/defschema PermissionsResponse {:permissions [Permission]})
(s/defschema PermissionResponse {:permission Permission})

(s/defschema MessageResponse {:message s/Any})
(s/defschema ErrorResponse {:error String})

(s/defschema CountPayload {:success-count Integer :failure-count Integer})
(s/defschema CountMessageResponse {:message CountPayload})

(s/defschema LoginRequest {:login_id String :password String})
(s/defschema LoginResponse
  {:message String
  (s/optional-key :requires_password_reset) Boolean
   :user {:id Integer
          :username String
          :email String
          (s/optional-key :first_name) (s/maybe String)
          (s/optional-key :last_name) (s/maybe String)
          (s/optional-key :permissions) [String]}})
(s/defschema ForgotPasswordRequest {:login_id String})
(s/defschema ForgotPasswordResponse
  {:message String
   (s/optional-key :reset_url) String})
(s/defschema ResetPasswordRequest {:token String :password String})
(s/defschema UpdateUserRequest
  {(s/optional-key :username) String
   (s/optional-key :email) String
   (s/optional-key :first_name) String
   (s/optional-key :last_name) String})
(s/defschema CreateUserRequest
  {:username String
   :email String
   :password String
   :first_name (s/maybe String)
   :last_name (s/maybe String)})
(s/defschema RolesPayload {:roles [Integer]})
(s/defschema UsersPayload {:user-ids [Integer]})
(s/defschema RoleUpdateRequest
  {(s/optional-key :name) String
   (s/optional-key :description) String
   (s/optional-key :users) [{:id Integer}]})
(s/defschema CreateRoleRequest {:name String :description (s/maybe String)})
(s/defschema CreatePermissionRequest {:name String :description (s/maybe String)})
(s/defschema PermissionUpdateRequest
  {(s/optional-key :name) String
   (s/optional-key :description) String})
(s/defschema NamePayload {:name String})
(s/defschema DescriptionPayload {:description String})
(s/defschema PasswordPayload {:password String})

(s/defschema OAuthErrorResponse {:error String})
(s/defschema OAuthTokenResponse
  {:access_token String
   :token_type String
   :expires_in Integer
   (s/optional-key :refresh_token) String
  (s/optional-key :id_token) String
   (s/optional-key :scope) String})
(s/defschema OAuthIntrospectionResponse
  {(s/optional-key :active) Boolean
   (s/optional-key :client_id) s/Any
   (s/optional-key :username) String
   (s/optional-key :scope) String
   (s/optional-key :token_type) String
   (s/optional-key :exp) Integer
   (s/optional-key :iat) Integer
   (s/optional-key :sub) String
   (s/optional-key :token_use) String})
(s/defschema OAuthMetadataResponse
  {:issuer String
   :authorization_endpoint String
   :token_endpoint String
   :introspection_endpoint String
   :revocation_endpoint String
   :response_types_supported [String]
   :grant_types_supported [String]
   :token_endpoint_auth_methods_supported [String]})

(s/defschema OpenIDConfigurationResponse
  {:issuer String
   :authorization_endpoint String
   :token_endpoint String
  :userinfo_endpoint String
  :end_session_endpoint String
  :jwks_uri String
   :response_types_supported [String]
   :grant_types_supported [String]
  :id_token_signing_alg_values_supported [String]
   :subject_types_supported [String]
   :scopes_supported [String]
   :claims_supported [String]
   :token_endpoint_auth_methods_supported [String]})

(s/defschema Jwk
  {:kty String
  :kid String
  :use String
  :alg String
  :n String
  :e String})

(s/defschema JwksResponse {:keys [Jwk]})

(s/defschema OpenIDUserInfoResponse
  {:sub String
  (s/optional-key :user_id) String
  (s/optional-key :username) String
  (s/optional-key :preferred_username) String
  (s/optional-key :email) String
  (s/optional-key :first_name) (s/maybe String)
  (s/optional-key :last_name) (s/maybe String)
  (s/optional-key :roles) [String]
  (s/optional-key :permissions) [String]})

(s/defschema OAuthLoginContextResponse {:message String :query-string (s/maybe String)})

(s/defschema OAuthScope {:id Integer :name String :description (s/maybe String)})
(s/defschema OAuthScopeResponse {:scope OAuthScope})
(s/defschema OAuthScopesResponse {:scopes [OAuthScope]})
(s/defschema OAuthScopeCreateRequest
  {:name String
   :description (s/maybe String)})

(s/defschema OAuthClient
  {:id Integer
   :client_id String
   :client_name String
   :token_endpoint_auth_method String
   :is_confidential Boolean
   :redirect_uris [String]
   :grants [String]
   :scopes [String]
   :created_at s/Any
   :updated_at s/Any})

(s/defschema OAuthClientResponse {:client OAuthClient})
(s/defschema OAuthClientsResponse {:clients [OAuthClient]})

(s/defschema OAuthClientCreateRequest
  {:client_id String
   :client_name String
   (s/optional-key :client_secret) String
   (s/optional-key :token_endpoint_auth_method) String
   (s/optional-key :is_confidential) Boolean
   (s/optional-key :redirect_uris) [String]
   (s/optional-key :grants) [String]
   (s/optional-key :scopes) [String]})

(s/defschema OAuthClientUpdateRequest
  {(s/optional-key :client_name) String
   (s/optional-key :client_secret) String
   (s/optional-key :token_endpoint_auth_method) String
   (s/optional-key :is_confidential) Boolean
   (s/optional-key :redirect_uris) [String]
   (s/optional-key :grants) [String]
   (s/optional-key :scopes) [String]})

(s/defschema OAuthClientCreateResponse
  {:client OAuthClient
   (s/optional-key :client_secret) String})
