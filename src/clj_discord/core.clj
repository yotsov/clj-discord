(ns clj-discord.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [gniazdo.core :as ws]))


(def the-token (atom nil))
(def the-gateway (atom nil))
(def the-socket (atom nil))
(def the-ready (atom nil))
(def the-keepalive (atom nil))


(defn obtain-token [email password]
  (let [response (client/post "https://discordapp.com/api/auth/login"
                              {:body (json/write-str {:email email :password password})
                               :content-type :json
                               :accept :json})
        status (:status response)]
    (if (= 200 status)
      (get (json/read-str (:body response)) "token")
      (println "Token obtention failed with status code " status))))


(defn obtain-gateway [token]
  (let [response (client/get "https://discordapp.com/api/gateway"
                             {:headers {:authorization token}})
        status (:status response)]
    (if (= 200 status)
      (get (json/read-str (:body response)) "url")
      (println "Gateway obtention failed with status code " status))))


(defn disconnect []
  (reset! the-keepalive false)
  (if (not (nil? @the-socket)) (ws/close @the-socket))
  (reset! the-token nil)
  (reset! the-gateway nil)
  (reset! the-socket nil)
  (reset! the-ready nil))


(defn connect [email password functions]
  (disconnect)
  (reset! the-keepalive true)
  (reset! the-token (obtain-token email password))
  (reset! the-gateway (obtain-gateway @the-token))
  (reset! the-socket
          (ws/connect 
            @the-gateway
            :on-receive #(let [received (json/read-str %)
                               t (get received "t")
                               d (get received "d")]
                           (if (.equals "READY" t) (reset! the-ready d))
                           (doseq [f (get functions t (get functions "OTHER" []))] (f t d))
                           (if (.equals "RESUMED" t) (connect email password functions)))))
  (ws/send-msg @the-socket (json/write-str {:op 2, :d {:token @the-token,:properties {:$browser "clj-discord"}}}))
  (.start (Thread. (fn [] 
                     (while (and @the-keepalive (nil? @the-ready)) (Thread/sleep 100))
                     (if @the-keepalive (println "Connected to Discord."))
                     (while @the-keepalive
                       (do
                         (ws/send-msg @the-socket (json/write-str {:op 1, :d (System/currentTimeMillis)}))
                         (Thread/sleep (get @the-ready "heartbeat_interval"))))))))


(defn post-message-with-mentions [channel_id message mentions]
  (client/post (str "https://discordapp.com/api/channels/" channel_id "/messages")
               {:body (json/write-str {:content message
                                       :mentions mentions
                                       :nonce (str (System/currentTimeMillis))
                                       :tts false})
                :headers {:authorization @the-token}
                :content-type :json
                :accept :json}))


(defn post-message [channel_id message]
  (post-message-with-mentions channel_id message '()))


(defn post-message-with-mention [channel_id message user_id]
  (post-message-with-mentions channel_id (str "<@" user_id "> " message) (list user_id)))




