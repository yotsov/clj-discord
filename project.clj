(defproject clj-discord "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [clj-http "3.12.3"]
                 [org.clojure/data.json "2.4.0"]
                 [stylefruits/gniazdo "1.0.0"]]

  :plugins [[lein-ancient "1.0.0-RC3"] ;; finds updatable dependencies
            [lein-cljfmt "0.8.0"] ;; for formatting Clojure code
            [jonase/eastwood "1.0.0"] ;; a Clojure linter
            [lein-kibit "0.1.8"] ;; another linter, for both Clojure and ClojureScript
            [com.github.clj-kondo/lein-clj-kondo "0.1.3"] ;; and one more linter, for both Clojure and ClojureScript
            [org.clojure/clojure "1.10.3"]] ;; making sure the plugins use the latest Clojure

  :main clj-discord.example
  :aot :all)
