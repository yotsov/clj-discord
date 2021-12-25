(ns clj-discord.example
  (:gen-class)
  (:require [clj-discord.core :as discord]))

(defonce token (.trim (slurp "token.txt")))

(defn d100 [_ data]
  (discord/answer-command data "!d100" (str "Here you are a random number between 1 and 100: " (inc (rand-int 100)))))

(defn log-event [type data]
  (println "\nReceived: " type " -> " data))

(defn -main []
  (discord/connect {:token token
                    :functions {"MESSAGE_CREATE" [d100]
                                "MESSAGE_UPDATE" [d100]
                                "ALL_OTHER" [log-event]}}))

;(discord/disconnect)
