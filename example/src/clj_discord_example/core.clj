(ns clj-discord-example.core
  (:gen-class)
  (:require [clj-discord.core :as discord]))

(defonce token (.trim (slurp "token.txt")))

(defn d100 [type data]
  (discord/answer-command data "!d100" (str "Here you are a random number between 1 and 100: " (+ (rand-int 100) 1))))

(defn log-event [type data] 
  (println "\nReceived: " type " -> " data))

(defn -main [& args]
  (discord/connect {:token token 
                    :functions {"MESSAGE_CREATE" [d100]
                                "MESSAGE_UPDATE" [d100]
                                ; "ALL_OTHER" [log-event]
                                }}))

;(discord/disconnect)