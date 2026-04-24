(ns lib.env
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- parse-line [line]
  (let [trimmed (str/trim line)]
    (when (and (seq trimmed) (not (str/starts-with? trimmed "#")))
      (let [idx (str/index-of trimmed "=")]
        (when (pos? (or idx -1))
          [(keyword (str/trim (subs trimmed 0 idx)))
           (str/trim (subs trimmed (inc idx)))])))))

(defn- load-dotenv []
  (let [f (io/file ".env")]
    (when (.exists f)
      (->> (str/split-lines (slurp f))
           (keep parse-line)
           (into {})))))

(defn- os-env []
  (->> (System/getenv)
       (into {} (map (fn [[k v]] [(keyword k) v])))))

;; OS environment variables take precedence over .env file values.
;; This allows production deployments to override via process environment
;; while .env provides defaults for local development.
(def _ (merge (load-dotenv) (os-env)))
