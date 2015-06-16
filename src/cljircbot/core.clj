(ns cljircbot.core
  (:gen-class)
  (:require [clojure.java.jdbc :as jdbc]
            [cljircbot.irc :as irc]
            [clojure.java.io :as io]
            [cljircbot.db :as db]))

(def db-file "./log")
(def cfg-file "config.clj")

(def default-cfg [{:name "quakenet"
                   :host "irc.quakenet.org"
                   :port 6667
                   :channels [["#titlebot123" nil] ["#titlebot1234" "password"]]
                   :user {:username "bot"
                          :nick "titlebot"
                          :realname "bot"}}])

(defn write-config []
  (spit cfg-file default-cfg))

(defn connect-network [network]
  (jdbc/with-db-connection [dbconn (db/h2 db-file)]
    (db/create-tables dbconn)
    (irc/connect network dbconn)))

(defn -main []
  (if (not (.exists (io/as-file cfg-file)))
    (do (println (str "Config file does not exist. Writing default config to " cfg-file))
        (write-config)))
  (let [config (eval (read-string (slurp cfg-file)))]
    ;; TODO: support for multiple concurrent network connections
    (connect-network (first config)))) 
