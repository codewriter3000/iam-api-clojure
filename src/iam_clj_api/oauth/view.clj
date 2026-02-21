(ns iam-clj-api.oauth.view
  (:require [compojure.api.sweet :refer :all]
            [iam-clj-api.oauth.controller :as controller]
            [iam-clj-api.schemas :as schemas]))

(defroutes oauth-view-routes
  (context "/oauth" []
    :tags ["OAuth"]
    (GET "/authorize" request
      :summary "OAuth authorization endpoint"
      :responses {302 {:description "Redirect to client with authorization code"}
                  403 {:schema schemas/ErrorResponse}
                  400 {:schema schemas/ErrorResponse}}
      (controller/authorize request))

    (GET "/login" request
      :summary "OAuth login context endpoint"
      :responses {200 {:schema schemas/OAuthLoginContextResponse}}
      (controller/oauth-login-context request))

    (POST "/login" request
      :summary "OAuth login endpoint"
      :responses {302 {:description "Redirects back to authorization endpoint"}
                  403 {:schema schemas/ErrorResponse}
                  401 {:schema schemas/ErrorResponse}}
      (controller/oauth-login request))

    (POST "/token" request
      :summary "OAuth token endpoint"
      :responses {200 {:schema schemas/OAuthTokenResponse}
                  400 {:schema schemas/OAuthErrorResponse}
                  401 {:schema schemas/OAuthErrorResponse}}
      (controller/token request))

    (POST "/introspect" request
      :summary "OAuth introspection endpoint"
      :responses {200 {:schema schemas/OAuthIntrospectionResponse}
                  401 {:schema schemas/OAuthErrorResponse}}
      (controller/introspect request))

    (POST "/revoke" request
      :summary "OAuth revocation endpoint"
      :responses {200 {:schema schemas/MessageResponse}
                  401 {:schema schemas/OAuthErrorResponse}}
      (controller/revoke request))

    (GET "/userinfo" request
      :summary "OpenID UserInfo endpoint"
      :responses {200 {:schema schemas/OpenIDUserInfoResponse}
                  401 {:schema schemas/OAuthErrorResponse}}
      (controller/userinfo request))

    (GET "/logout" request
      :summary "OpenID Provider logout endpoint"
      :responses {302 {:description "Session cleared and redirected to client login"}}
      (controller/oauth-logout request)))

  (GET "/.well-known/oauth-authorization-server" request
    :tags ["OAuth"]
    :summary "OAuth authorization server metadata"
    :responses {200 {:schema schemas/OAuthMetadataResponse}}
    (controller/metadata request))

  (GET "/.well-known/openid-configuration" request
    :tags ["OAuth"]
    :summary "OpenID Provider Configuration"
    :responses {200 {:schema schemas/OpenIDConfigurationResponse}}
    (controller/openid-configuration request))

  (GET "/.well-known/jwks.json" request
    :tags ["OAuth"]
    :summary "JSON Web Key Set"
    :responses {200 {:schema schemas/JwksResponse}}
    (controller/jwks request)))
