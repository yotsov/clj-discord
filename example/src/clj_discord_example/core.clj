(ns clj-discord-example.core
  (:require [clj-discord.core :as discord]))

;; not committing "credentials.txt" because clojurecup 2015 repos are public
(def email    (first (.split (slurp "credentials.txt") "/")))
(def password (last  (.split (slurp "credentials.txt") "/")))
