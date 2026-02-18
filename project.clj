(defproject iam-clj-api "0.1.0-SNAPSHOT"
  :main iam-clj-api.handler
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [compojure "1.7.1"]
                 [com.stuartsierra/component "1.1.0"]
                 [ring/ring-jetty-adapter "1.14.1"]
                 [ring/ring-core "1.13.0"]
                 [ring/ring-ssl "0.4.0"]
                 [ring/ring-headers "0.4.0"]
                 [ring/ring-anti-forgery "1.4.0"]
                 [org.ring-clojure/ring-websocket-middleware "0.2.1"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors/ring-cors "0.1.9"]
                 [ring/ring-devel "1.14.1"]
                 [instaparse "1.5.0"]
                 ; DB
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [org.postgresql/postgresql "42.7.4"]

                 [buddy/buddy-hashers "1.4.0"]
                 [com.sun.mail/jakarta.mail "2.0.1"]
                 [environ "1.2.0"]
                 ; Logging
                 [org.clojure/tools.logging "1.3.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.24.3"]
                 [org.apache.logging.log4j/log4j-api "2.24.3"]
                 [org.apache.logging.log4j/log4j-core "2.24.3"]
                 [metosin/compojure-api "2.0.0-alpha33"]
                 [metosin/ring-swagger-ui "5.20.0"]
                 [metosin/ring-http-response "0.9.5"]
                 [cider/cider-nrepl "0.55.7"]]

  :plugins [[lein-ring "0.12.5"]
            [lein-environ "1.2.0"]
            [cider/cider-nrepl "0.55.7"]]
  :ring {:handler iam-clj-api.handler/app
         :port 8080}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.4.0"]]}

             :cider {:dependencies [[javax.servlet/servlet-api "2.5"]
                                    [ring/ring-mock "0.4.0"]]
                     :plugins [[cider/cider-nrepl "0.55.7"]]}})
