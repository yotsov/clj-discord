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


(defn connect [email password]
  (disconnect)
  (reset! the-keepalive true)
  (reset! the-token (obtain-token email password))
  (reset! the-gateway (obtain-gateway @the-token))
  (reset! the-socket
          (ws/connect 
            @the-gateway
            :on-receive #(do 
                           (println %)
                           (if (.equals "READY" (get (json/read-str %) "t"))
                             (reset! the-ready (json/read-str %))))))
  (ws/send-msg @the-socket (json/write-str {:op 2, :d {:token @the-token,:properties {:$browser "clj-discord"}}}))
  (while (and @the-keepalive (nil? @the-ready)) (Thread/sleep 1000))
  (.start (Thread. (fn [] (while @the-keepalive
                            (do
                              (ws/send-msg @the-socket (json/write-str {:op 1, :d (System/currentTimeMillis)}))
                              (Thread/sleep (get (get @the-ready "d") "heartbeat_interval")))))))
  (println "Connected."))


(defn disconnect []
  (reset! the-keepalive false)
  (if (not (nil? @the-socket)) (ws/close @the-socket))
  (reset! the-token nil)
  (reset! the-gateway nil)
  (reset! the-socket nil)
  (reset! the-ready nil))

