(ns iam-clj-api.auth.email
  (:require [clojure.tools.logging :as log]
            [env :as env])
  (:import [java.util Properties]
           [jakarta.mail Message$RecipientType Session Transport]
           [jakarta.mail.internet InternetAddress MimeMessage]))

(defn- bool-from-string [value]
  (= "true" (some-> value str clojure.string/lower-case)))

(defn- smtp-config []
  {:host (get env/_ :SMTP_HOST)
   :port (or (get env/_ :SMTP_PORT) "587")
   :username (get env/_ :SMTP_USER)
   :password (get env/_ :SMTP_PASS)
   :from (or (get env/_ :SMTP_FROM) "no-reply@localhost")
   :starttls (bool-from-string (or (get env/_ :SMTP_STARTTLS) "true"))
   :auth (bool-from-string (or (get env/_ :SMTP_AUTH) "true"))})

(defn send-password-reset-email [to-email reset-url]
  (let [{:keys [host port username password from starttls auth]} (smtp-config)]
    (if (or (nil? host) (nil? username) (nil? password))
      (do
        (log/warn "SMTP config not fully set; password reset URL:" reset-url "for" to-email)
        {:status :skipped})
      (let [props (doto (Properties.)
                    (.put "mail.smtp.host" host)
                    (.put "mail.smtp.port" (str port))
                    (.put "mail.smtp.auth" (str auth))
                    (.put "mail.smtp.starttls.enable" (str starttls)))
            session (Session/getInstance props)
            message (doto (MimeMessage. session)
                      (.setFrom (InternetAddress. from))
                      (.setRecipients Message$RecipientType/TO (InternetAddress/parse to-email))
                      (.setSubject "Password Reset Request")
                      (.setText (str "A password reset was requested for your account.\n\n"
                                     "Open this URL to reset your password:\n"
                                     reset-url "\n\n"
                                     "If you did not request this, you can ignore this email.")))]
        (Transport/send message username password)
        {:status :sent}))))
