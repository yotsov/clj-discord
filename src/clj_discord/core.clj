(ns clj-discord.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

;; not committing "credentials.txt" because clojurecup 2015 repos are public
(def email    (first (.split (slurp "credentials.txt") "/")))
(def password (last  (.split (slurp "credentials.txt") "/")))

(defn obtain-token []
  (let [response (client/post "https://discordapp.com/api/auth/login"
                              {:body (json/write-str {:email email :password password})
                               :content-type :json
                               :accept :json})
        status (:status response)]
    (if (= 200 status)
      (get (json/read-str (:body response)) "token")
      (println "Token obtention failed with status code " status))))

(defn obtain-gateway []
  (let [response (client/get "https://discordapp.com/api/gateway"
              {:headers {:authorization (obtain-token)}})
        status (:status response)]
      (if (= 200 status)
      (get (json/read-str (:body response)) "url")
      (println "Gateway obtention failed with status code " status))))
