(ns clj-discord.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [gniazdo.core :as ws])
  (:import (org.eclipse.jetty.util.ssl SslContextFactory)
           (org.eclipse.jetty.websocket.client WebSocketClient)))

(def default-max-text-message-size (* 64 1024))
(defn default-log-function [& args] (println "\n" args))
(def default-log-events? true)

(defonce the-token (atom nil))
(defonce the-gateway (atom nil))
(defonce the-socket (atom nil))
(defonce the-heartbeat-interval (atom nil))
(defonce the-seq (atom nil))
(defonce the-keepalive (atom false))

(defn disconnect []
  (reset! the-keepalive false)
  (if (not (nil? @the-socket)) (ws/close @the-socket))
  (reset! the-token nil)
  (reset! the-gateway nil)
  (reset! the-socket nil)
  (reset! the-seq nil)
  (reset! the-heartbeat-interval nil))

(defn connect
  ([token functions]
    (connect token functions default-log-events? default-log-function default-max-text-message-size false))
  ([token functions log-events?]
    (connect token functions log-events? default-log-function default-max-text-message-size false))
  ([token functions log-events? max-text-message-size]
    (connect token functions log-events? default-log-function max-text-message-size false))
  ([token functions log-events? log-function max-text-message-size]  
    (connect functions log-events? log-function max-text-message-size false))
  ([token functions log-events? log-function max-text-message-size reconnecting?]
    (if reconnecting? (do 
                        (disconnect)
                        (Thread/sleep 5000)))
    (reset! the-keepalive true)
    (reset! the-token (str "Bot " token))
    (reset! the-gateway (str
                          (get
                            (json/read-str
                              (:body (http/get "https://discordapp.com/api/gateway"
                                               {:headers {:authorization @the-token}})))
                            "url")
                          "?v=6&encoding=json"))
    (reset! the-socket
            (let [client (new WebSocketClient (new SslContextFactory))]
              (.setMaxTextMessageSize (.getPolicy client) max-text-message-size)
              (.start client)
              (ws/connect
                @the-gateway
                :client client
                :on-receive #(let [received (json/read-str %)
                                   logevent (if log-events? (log-function %))
                                   op (get received "op")
                                   type (get received "t")
                                   data (get received "d")
                                   seq (get received "s")]
                               (if (= 10 op) (reset! the-heartbeat-interval (get data "heartbeat_interval")))
                               (if (not (nil? seq)) (reset! the-seq seq))
                               (if (not (nil? type)) (doseq [afunction (get functions type (get functions "ALL_OTHER" []))]
                                                       (afunction type data)))))))
    (.start (Thread. (fn []
                       (while @the-keepalive
                         (try
                           (if (nil? @the-heartbeat-interval)
                             (Thread/sleep 100)
                             (do
                               (if log-events? (log-function "Sending heartbeat " @the-seq))
                               (ws/send-msg @the-socket (json/write-str {:op 1, :d @the-seq}))
                               (Thread/sleep @the-heartbeat-interval)
                               ))
                           (catch Exception e (do
                                                (log-function "Caught exception: " (.getMessage e))
                                                (reset! the-keepalive false)
                                                )))))))
    (Thread/sleep 1000)
    (ws/send-msg @the-socket (json/write-str {:op 2, :d {"token" @the-token
                                                         "properties" {"$os" "linux"
                                                                       "$browser" "clj-discord"
                                                                       "$device" "clj-discord"
                                                                       "$referrer" ""
                                                                       "$referring_domain" ""}
                                                         "compress" false}}))
    (while @the-keepalive (Thread/sleep 1000))
    (try
      (connect token functions log-events? log-function max-text-message-size true)
      (catch Exception e 
        (do
          (log-function "Caught exception: " (.getMessage e))
          (Thread/sleep 60000)
          (try
            (connect token functions log-events? log-function max-text-message-size true)
            (catch Exception e 
              (do
                (log-function "Caught exception: " (.getMessage e))
                (log-function "Abandoning!")
                (disconnect)
                ))))))))

(defn connect-without-blocking
  ([token functions]
    (connect-without-blocking token functions default-log-events? default-log-function default-max-text-message-size))
  ([token functions log-events?]
    (connect-without-blocking token functions log-events? default-log-function default-max-text-message-size))
  ([token functions log-events? max-text-message-size]
    (connect-without-blocking token functions log-events? default-log-function max-text-message-size))
  ([token functions log-events? log-function max-text-message-size]
    ((.start (Thread. (fn [] (connect token functions log-events? log-function max-text-message-size)))))))

(defn post-message [channel-id message]
  (http/post (str "https://discordapp.com/api/channels/" channel-id "/messages")
             {:body (json/write-str {:content message
                                     :nonce (str (System/currentTimeMillis))
                                     :tts false})
              :headers {:authorization @the-token}
              :content-type :json
              :accept :json}))

(defn post-message-with-file [channel-id message filename]
  (http/post (str "https://discordapp.com/api/channels/" channel-id "/messages")
             {:multipart [{:name "content" :content message}
                          {:name "nonce" :content (str (System/currentTimeMillis))}
                          {:name "tts" :content "false"}
                          {:name filename :part_name "file" :content (io/file filename)}]
              :headers {:authorization @the-token}}))

(defn post-message-with-mention [channel-id message user-id]
  (post-message channel-id (str "<@" user-id ">" message)))

(defn answer-command [data command answer]
  (if (= command (get data "content"))
    (post-message-with-mention
      (get data "channel_id")
      (str " " answer)
      (get (get data "author") "id"))))
