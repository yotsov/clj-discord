(defproject clj-discord-example "0.1.0-SNAPSHOT"
  :dependencies [[clj-discord "0.1.0-SNAPSHOT"]]
  :main clj-discord-example.core
  :profiles {:uberjar {:aot [clj-discord-example.core]}})
