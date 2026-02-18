(ns iam-clj-api.schemas
  (:require [schema.core :as s]))

(s/defschema User
  {:id Integer
   :username String
   :email String
   (s/optional-key :first_name) (s/maybe String)
   (s/optional-key :last_name) (s/maybe String)
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

(s/defschema LoginRequest {:username String :password String})
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
