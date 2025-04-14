(ns lib.response)

(defn error [status msg]
  {:status status :error (str "{\"error\": \"" msg "\"}")})

(defn success [status msg]
  {:status status :body (str "{\"message\": \"" msg "\"}")})

(defn work [status data]
  {:status status :body data})