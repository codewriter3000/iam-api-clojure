(ns iam-clj-api.oauth.crypto
  (:require [buddy.hashers :as hashers])
  (:import (java.security SecureRandom MessageDigest)
           (java.util Base64)))

(def ^:private secure-random (SecureRandom.))

(defn random-token
  ([n-bytes]
   (let [bytes (byte-array n-bytes)]
     (.nextBytes secure-random bytes)
     (.encodeToString (Base64/getUrlEncoder) bytes)))
  ([] (random-token 48)))

(defn sha256 [value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str value) "UTF-8"))]
    (format "%064x" (java.math.BigInteger. 1 digest))))

(defn derive-secret [secret]
  (hashers/derive secret))

(defn check-secret [secret secret-hash]
  (and secret secret-hash (hashers/check secret secret-hash)))
