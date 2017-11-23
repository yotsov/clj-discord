(ns clj-discord.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [gniazdo.core :as ws])
  (:import (org.eclipse.jetty.util.ssl SslContextFactory)
           (org.eclipse.jetty.websocket.client WebSocketClient)))

(defonce previous-bot (atom -1))
(defonce bots (atom {}))

(defn disconnect
  ([]
    (disconnect [0]))
  ([bot]
    (if (contains? @bots bot)
      (let [socket (:socket (get @bots bot))
            websocket-client (:websocket-client (get @bots bot))]
        (if (not (nil? socket)) (ws/close socket))
        (if (not (nil? websocket-client)) (.stop websocket-client))
        (swap! bots dissoc bot)))))

(defn connect [params]
  (let [{:keys [token functions log-events? log-function max-text-message-size bot]
         :or {functions {}
              log-events? true
              log-function (fn [& args] (println "\n" args))
              max-text-message-size (* 64 1024)
              bot (swap! previous-bot inc)}} params
        token (if (.startsWith token "Bot ") (.trim token) (str "Bot " (.trim token)))
        params {:token token
                :functions functions
                :log-events? log-events?
                :log-function log-function
                :max-text-message-size max-text-message-size
                :bot bot}

        websocket-client (new WebSocketClient (new SslContextFactory))
        heartbeat-thread (Thread. (fn []
                                    (while (:keep-alive (get @bots bot))
                                      (try
                                        (if (nil? (:heartbeat-interval (get @bots bot)))
                                          (Thread/sleep 100)
                                          (do
                                            (if log-events? (log-function "Sending heartbeat " (:seq (get @bots bot))))
                                            (ws/send-msg (:socket (get @bots bot)) (json/write-str {:op 1, :d (:seq (get @bots bot))}))
                                            (Thread/sleep (:heartbeat-interval (get @bots bot)))
                                            ))
                                        (catch Exception e (do
                                                             (log-function "Caught exception: " (.getMessage e))
                                                             (swap! bots update-in [bot] assoc :keep-alive false)
                                                             ))))))]

    (if (contains? @bots bot) (do
                                (disconnect bot)
                                (Thread/sleep 5000)))

    (swap! bots assoc bot {:params params
                           :keep-alive true
                           :gateway (str
                                      (get
                                        (json/read-str
                                          (:body (http/get "https://discordapp.com/api/gateway"
                                                           {:headers {:authorization token}})))
                                        "url")
                                      "?v=6&encoding=json")
                           :websocket-client websocket-client
                           :heartbeat-thread heartbeat-thread
                           :control-thread (Thread/currentThread)})

    (.setMaxTextMessageSize (.getPolicy websocket-client) max-text-message-size)
    (.start websocket-client)

    (swap! bots update-in [bot] assoc :socket
           (ws/connect
             (:gateway (get @bots bot))
             :client
             websocket-client
             :on-receive
             #(let [received (json/read-str %)
                    logevent (if log-events? (log-function %))
                    op (get received "op")
                    type (get received "t")
                    data (get received "d")
                    seq (get received "s")]
                (if (= 10 op) (swap! bots update-in [bot] assoc :heartbeat-interval (get data "heartbeat_interval")))
                (if (not (nil? seq)) (swap! bots update-in [bot] assoc :seq seq))
                (if (not (nil? type)) (doseq [afunction (get functions type (get functions "ALL_OTHER" []))]
                                        (afunction type data))))))

    (.start heartbeat-thread)
    (Thread/sleep 1000)

    (ws/send-msg (:socket (get @bots bot)) (json/write-str {:op 2, :d {"token" token
                                                                       "properties" {"$os" "linux"
                                                                                     "$browser" "clj-discord"
                                                                                     "$device" "clj-discord"
                                                                                     "$referrer" ""
                                                                                     "$referring_domain" ""}
                                                                       "compress" false}}))

    (while (:keep-alive (get @bots bot)) (Thread/sleep 1000))
    (try
      (connect params)
      (catch Exception e
        (do
          (log-function "Caught exception: " (.getMessage e))
          (Thread/sleep 60000)
          (try
            (connect params)
            (catch Exception e
              (do
                (log-function "Caught exception: " (.getMessage e))
                (log-function "Abandoning the attempts to reconnect bot " bot)
                (disconnect bot)
                ))))))))

(defn connect-without-blocking [params]
  (let [params-contain-bot (contains? params :bot)
        bot (if params-contain-bot (:bot params) (swap! previous-bot inc))
        checking-bot (if (not (integer? bot)) (throw (Exception. "Malformed bot identifier!")))
        params (assoc params :bot bot)]
    (.start (Thread. (fn [] (connect params))))
    bot))

(defn check-rate-limit [bot]
  (let [now (/ (System/currentTimeMillis) 1000)
        previous-activity (:activity (get @bots bot))
        previous-activity (if (nil? previous-activity) 0 previous-activity)
        seconds-since-previous-activity (- now previous-activity)]
    (if
      (> 5 seconds-since-previous-activity)
      false
      (do
        (swap! bots update-in [bot] assoc :activity now)
        true))))

(defn post-message
  ([channel-id message]
    (post-message 0 channel-id message))
  ([bot channel-id message]
    (if (check-rate-limit bot)
      (http/post (str "https://discordapp.com/api/channels/" channel-id "/messages")
                 {:body (json/write-str {:content message
                                         :nonce (str (System/currentTimeMillis))
                                         :tts false})
                  :headers {:authorization (:token (:params (get @bots bot)))}
                  :content-type :json
                  :accept :json}))))

(defn post-message-with-file
  ([channel-id message filename]
    (post-message-with-file 0 channel-id message filename))
  ([bot channel-id message filename]
    (if (check-rate-limit bot)
      (http/post (str "https://discordapp.com/api/channels/" channel-id "/messages")
                 {:multipart [{:name "content" :content message}
                              {:name "nonce" :content (str (System/currentTimeMillis))}
                              {:name "tts" :content "false"}
                              {:name filename :part_name "file" :content (io/file filename)}]
                  :headers {:authorization (:token (:params (get @bots bot)))}}))))

(defn post-message-with-mention
  ([channel-id message user-id]
    (post-message-with-mention 0 channel-id message user-id))
  ([bot channel-id message user-id]
    (post-message bot channel-id (str "<@" user-id ">" message))))

(defn answer-command
  ([data command answer]
    (answer-command 0 data command answer))
  ([bot data command answer]
    (if (= command (get data "content"))
      (post-message-with-mention
        bot
        (get data "channel_id")
        (str " " answer)
        (get (get data "author") "id")))))

(defn delete-message
  ([data command]
   (delete-message 0 data command))
  ([bot data command]
   (let [channel_id (get data "channel_id") message (get data "id")]
     (if (and (check-rate-limit bot) (= command (get data "content")))
       (http/delete (str "https://discordapp.com/api/channels/" channel_id "/messages/" message "?token=" (:token (:params (get @bots bot)))) {:throw-exceptions false})))))
