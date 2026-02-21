(ns iam-clj-api.oauth.admin.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.oauth.admin.controller :as controller]
            [iam-clj-api.schemas :as schemas]))

(defroutes oauth-admin-view-routes
  (context "/oauth" []
    :tags ["OAuth Admin"]
    (GET "/scope" request
      :summary "List OAuth scopes"
      :responses {200 {:schema schemas/OAuthScopesResponse}
                  403 {:schema schemas/ErrorResponse}}
      (controller/list-scopes request))

    (POST "/scope" request
      :summary "Create OAuth scope"
      :body [payload schemas/OAuthScopeCreateRequest]
      :responses {201 {:schema schemas/OAuthScopeResponse}
                  400 {:schema schemas/ErrorResponse}
                  403 {:schema schemas/ErrorResponse}}
      (controller/create-scope request payload))

    (DELETE "/scope/:name" [name :as request]
      :summary "Delete OAuth scope"
      :responses {204 {:schema schemas/MessageResponse}
                  403 {:schema schemas/ErrorResponse}
                  404 {:schema schemas/ErrorResponse}}
      (controller/delete-scope request name))

    (GET "/client" request
      :summary "List OAuth clients"
      :responses {200 {:schema schemas/OAuthClientsResponse}
                  403 {:schema schemas/ErrorResponse}}
      (controller/list-clients request))

    (GET "/client/:id" [id :as request]
      :summary "Get OAuth client"
      :responses {200 {:schema schemas/OAuthClientResponse}
                  403 {:schema schemas/ErrorResponse}
                  404 {:schema schemas/ErrorResponse}}
      (controller/get-client request id))

    (POST "/client" request
      :summary "Create OAuth client"
      :body [payload schemas/OAuthClientCreateRequest]
      :responses {201 {:schema schemas/OAuthClientCreateResponse}
                  400 {:schema schemas/ErrorResponse}
                  403 {:schema schemas/ErrorResponse}}
      (controller/create-client request payload))

    (PUT "/client/:id" [id :as request]
      :summary "Update OAuth client"
      :body [payload schemas/OAuthClientUpdateRequest]
      :responses {200 {:schema schemas/OAuthClientCreateResponse}
                  403 {:schema schemas/ErrorResponse}
                  404 {:schema schemas/ErrorResponse}}
      (controller/update-client request id payload))

    (DELETE "/client/:id" [id :as request]
      :summary "Delete OAuth client"
      :responses {204 {:schema schemas/MessageResponse}
                  403 {:schema schemas/ErrorResponse}
                  404 {:schema schemas/ErrorResponse}}
      (controller/delete-client request id))))
