(ns clj-discord-example.core
  (:gen-class)
  (:require [clj-discord.core :as discord]))

(def email    (first (.split (slurp "credentials.txt") "/")))
(def password (last  (.split (slurp "credentials.txt") "/")))

(defn answer-command [type data command answer]
  (if (= command (get data "content"))
    (discord/post-message-with-mention 
      (get data "channel_id") 
      answer
      (get (get data "author") "id"))))

(defn get-commands [type data]
  (answer-command type data "!commands" "Currently I can answer the following commands: !commands !id !random"))

(defn get-id [type data]
  (answer-command type data "!id" (str "Your Discord ID is: " (get (get data "author") "id"))))

(defn get-random [type data]
  (answer-command type data "!random" (str "Here you are a random number between 1 and 100: " (+ (rand-int 100) 1))))

(defn do-nothing [type data]
  nil)

(defn log-event [type data] 
  (println "Received: " type " -> " data))

(defn welcome-newcomer [type data]
  (Thread/sleep 3000)
  (discord/post-message-with-mention 
    "120276561306845184" 
    "Welcome; please type !commands and I will let you know what commands I accept."
    (get (get data "user") "id")))

(defn -main [& args]
  (discord/connect email password {"MESSAGE_CREATE" [get-commands get-id get-random]
                                   "MESSAGE_UPDATE" [get-commands get-id get-random]
                                   "GUILD_MEMBER_ADD" [welcome-newcomer]
                                   "READY" [do-nothing]
                                   "OTHER" [log-event]}))

;(discord/disconnect)



