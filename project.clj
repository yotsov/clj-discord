(defproject clj-discord "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [org.clojure/data.json "0.2.6"]
                 [stylefruits/gniazdo "0.4.1"]]
  :main clj-discord.core
  :profiles {:uberjar {:aot [clj-discord.core]}})
