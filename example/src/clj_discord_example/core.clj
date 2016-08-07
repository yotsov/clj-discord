(ns clj-discord-example.core
  (:gen-class)
  (:require [clj-discord.core :as discord]))

(defonce email    (.trim (first (.split (slurp "credentials.txt") "/"))))
(defonce password (.trim (last  (.split (slurp "credentials.txt") "/"))))

(defn answer-command [type data command answer]
  (if (= command (get data "content"))
    (discord/post-message-with-mention 
      (get data "channel_id") 
      answer
      (get (get data "author") "id"))))

(defn d100 [type data]
  (answer-command type data "!d100" (str " Here you are a random number between 1 and 100: " (+ (rand-int 100) 1))))

(defn r3d6 []
  (+ (rand-int 6) (rand-int 6) (rand-int 6) 3))

(defn char [type data]
  (answer-command type data "!s" 
                  (str " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) " " (r3d6) )
                  ))

(defn fixedchar [type data]
  (answer-command type data "!f" 
                  (str " STR:" (r3d6) " DEX:" (r3d6) " CON:" (r3d6) " INT:" (r3d6) " WIS:" (r3d6) " CHA:" (r3d6) " hotness:" (r3d6) " coolness:" (r3d6) " grubskill:" (r3d6) " richness:" (r3d6) )
                  ))

(defn person [type data]
  (answer-command type data "!p" 
                  (str " " 
                       (rand-nth ["male-without-facial-hair" "male-with-moustache" "male-with-beard-stub" "male-with-some-beard" "male-with-long-beard" "female" "female" "female" "female" "female" "of-unclear-sex"])
                       " "
                       (rand-nth ["dwarf" "elf" "half-ocr" "halfling" "gnome" "half-elf" "human" "human" "human" "human" "human" "human"])
                       " "
                       (rand-nth ["very-young" "of-prime-age" "past-prime-age" "middle-aged" "old" "very-old"])
                       " "
                       (rand-nth ["very-bad-mood" "somewhat-bad-mood" "neutral-mood" "good-mood" "very-good-mood"])
                       " " 
                       (rand-nth ["very-thin" "a-bit-thin" "average-weight" "plump" "fat"])
                       " "
                       (rand-nth ["short-hair" "long-hair"])
                       " "
                        (rand-nth ["not-talkative" "of-average-talkativeness" "long-winded"])
)))

(defn log-event [type data] 
  (println "Received: " type " -> " data))

(defn welcome-newcomer [type data]
  (Thread/sleep 3000)
  (discord/post-message-with-mention 
    "120276561306845184" 
    "Welcome!"
    (get (get data "user") "id")))

(defn -main [& args]
  (discord/connect email password {"MESSAGE_CREATE" [d100 char fixedchar person]
                                   "MESSAGE_UPDATE" [d100 char fixedchar person]
                                   "GUILD_MEMBER_ADD" [welcome-newcomer]
                                   "OTHER" [log-event]}))

;(discord/disconnect)



