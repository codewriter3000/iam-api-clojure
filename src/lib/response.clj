(ns lib.response)

(defn error [status msg]
  {:status status :body {:error msg}})

(defn success [status msg]
  {:status status :body {:message msg}})

(defn work [status data]
  {:status status :body data})