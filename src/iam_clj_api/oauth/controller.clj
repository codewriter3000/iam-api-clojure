(ns iam-clj-api.oauth.controller
  (:require [iam-clj-api.oauth.authorization :as authorization]
            [iam-clj-api.oauth.tokens :as tokens]))

(defn authorize [request]
  (authorization/authorize request))

(defn oauth-login [request]
  (authorization/oauth-login request))

(defn oauth-login-context [request]
  (authorization/oauth-login-context request))

(defn oauth-logout [request]
  (authorization/oauth-logout request))

(defn token [request]
  (tokens/token request))

(defn introspect [request]
  (tokens/introspect request))

(defn revoke [request]
  (tokens/revoke request))

(defn jwks [request]
  (tokens/jwks request))

(defn userinfo [request]
  (tokens/userinfo request))

(defn metadata [request]
  (tokens/metadata request))

(defn openid-configuration [request]
  (tokens/openid-configuration request))
