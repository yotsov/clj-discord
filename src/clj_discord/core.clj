(ns clj-discord.core)

;; not committing "credentials.txt" because clojurecup 2015 repos are public
(def username (first (.split (slurp "credentials.txt") "/")))
(def password (last  (.split (slurp "credentials.txt") "/")))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
