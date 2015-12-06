# clj-discord
Clojure library for using the the Discord API


To use this library:

1. clone this repository
2. do "lein install"
3. add to your project dependencies [clj-discord "0.1.0-SNAPSHOT"]
4. add to your namespace declaration (:require [clj-discord.core :as discord])


To connect to Discord, call (discord/connect email password functions) where:

1. email and password are the Discord credentials (you need to create a Discord account)
2. functions is a map from strings to seqs of functions, which might look something like this:

{"MESSAGE_CREATE" [get-commands get-id get-random]
 "MESSAGE_UPDATE" [get-commands get-id get-random]
 "GUILD_MEMBER_ADD" [welcome-newcomer]
 "READY" [do-nothing]
 "OTHER" [log-event]}
 
The map keys such as "MESSAGE_CREATE" and "MESSAGE_UPDATE" indicate the different types of events
that the Discord server could notify us of.

The functions such as "get-commands" and "get-id" will be called when an event of the corresponding
type occurs.

If the key "OTHER" exists, the functions it points to will be called on every event that is not present as a key in the map.

The functions need to take two parameters, such as in:

(defn log-event [type data] 
  (println "Received: " type " -> " data))
  
type is again the string indicating the type of event, such as "MESSAGE_CREATE" and "MESSAGE_UPDATE". We are passing it to the
function, because a function can be assigned to be executed for several different types of events, and inside it might need to know
the type of the current event.

data is a map that contains data relevant to the event type, which Discord has sent us. A full list of event types and the format of
data corresponding to each has not been made known by Discord yet, but here are several useful examples:

type -> data
-------------

GUILD_MEMBER_ADD  ->  {user {username gaga, id 123146380976848896, discriminator 6104, avatar nil}, roles [], joined_at 2015-12-06T19:39:45.191505+00:00, guild_id 120276561306845184}

PRESENCE_UPDATE  ->  {username gaga, status online, id 123146380976848896, game_id nil, discriminator 6104, avatar nil}

PRESENCE_UPDATE  ->  {status idle, id 118095874323775492, game_id nil}

PRESENCE_UPDATE  ->  {status offline, id 123146380976848896, game_id nil}

MESSAGE_CREATE  ->  {mention_everyone false, embeds [], mentions [], channel_id 120276561306845184, author {username gaga, id 123147067660042241, discriminator 9375, avatar nil}, id 123148066621620228, timestamp 2015-12-06T19:46:27.063000+00:00, content This is a message I wrote., tts false, nonce 4420017994214670336, edited_timestamp nil, attachments []}

MESSAGE_UPDATE  ->  {mention_everyone false, embeds [], mentions [], channel_id 120276561306845184, author {username stuckpanda, id 118095874323775492, discriminator 5806, avatar a92e3d8b198c22f41ddc087c8c9419d5}, id 123154536046002179, timestamp 2015-12-06T20:12:09.494000+00:00, content !commands, tts false, nonce nil, edited_timestamp 2015-12-06T20:12:35.481943+00:00, attachments []}

TYPING_START  ->  {user_id 123147067660042241, timestamp 1449431180, channel_id 120276561306845184}




The API also provides the possibility to create messages in Discord, for example: (discord/post-message "120276561306845184" "this is a chat message")

where "120276561306845184" is a channel_id, which we can obtain for example from the data of MESSAGE_CREATE event (allowing to automatically answer chat messages in the correct channel, for example)


To disconnect from Discord: (discord/disconnect)

For a detailed, tested example, check the example folder.

