(ns iam-clj-api.oauth.config)

(def issuer "http://localhost:8080")
(def frontend-oauth-login-url "http://localhost:3000/oauth/login")
(def access-token-ttl-seconds (* 15 60))
(def auth-code-ttl-seconds (* 5 60))
(def refresh-token-ttl-seconds (* 30 24 60 60))
(def id-token-ttl-seconds (* 15 60))
(def session-cookie-name "iam-session")
