(ns cljircbot.irc
  (:require [cljircbot.db :as db])
  (:import
   (java.net ServerSocket Socket)
   (java.io PrintWriter BufferedReader InputStreamReader)))

(def clientversion "titlebot 0.0.1")

(defn socket-reader [socket]
  (BufferedReader. (InputStreamReader. (.getInputStream socket))))

(defn socket-writer [socket]
  (PrintWriter. (.getOutputStream socket) true))                     

(defn split-words [words]
  (clojure.string/split words #" "))

(defn join-words [words]
  (clojure.string/join " " words))

(defn parse-msg [server-msg]
  (let [[w & ws :as words] (split-words server-msg)
        [prefix body] (if (= (first w) \:) [(subs w 1) ws] [nil words])
        cmd (first body)
        params (join-words (rest body))]
    {:text server-msg :prefix prefix :cmd cmd :params params}))

(defn parse-params [params]
  (if (not (empty? params))
    (if (= (first (first params)) \:)
      (list (subs (join-words params) 1))
      (cons (first params) (parse-params (rest params))))))

(defn parse-host [prefix]
  (let [[nick hostname] (clojure.string/split prefix #"!")]
    [nick hostname]))

(defn parse-privmsg [privmsg]
  (let [[nick host] (parse-host (privmsg :prefix))
        [target msg] (parse-params (split-words (privmsg :params)))]
    {:sender nick :target target :text msg}))

(defn make-privmsg [target msg]
  (format "PRIVMSG %s :%s" target msg))

(defn notice [target text]
  (format "NOTICE %s :%s" target text))  

(defn nick-msg [user]
  (format "NICK %s" (user :nick)))

(defn user-msg [user]
  (format "USER %s 8 * :%s" (user :username) (user :realname)))

(defn join-msg [channel key]
  (format "JOIN %s %s" channel (if (= key nil) "" key)))

(defn pong-msg [ping]
  (format "PONG %s" ping))

(defn is-url [text]
  (re-matches #"https?.*" text))

(defn parse-urls [msg]
  (filter is-url (split-words (msg :text))))

(defn log [dbconn msg]
  (db/add-msg dbconn (msg :text) (msg :sender) (msg :target)))

(defn urls-cmd
  "Find the last 5 urls sent to a given target."
  [dbconn msg]
  (let [cmd (split-words (msg :text))
        urls (if (> (count cmd) 1)
               (db/get-urls-by-date dbconn (msg :target) (second cmd))
               (db/get-urls dbconn 5 (msg :target)))]
    (map (fn [url] (url :address)) urls)))

(defn imgur-filter
  "Replace i.imgur.com/image.ext with imgur.com/image to get the image title"
  [urls]
  (let [authority #(.toLowerCase (.getAuthority (java.net.URI. %)))
        replace-imgur #(clojure.string/replace % #"i.imgur.com" "imgur.com")]
    (map (fn [url] (if (= (authority url) "i.imgur.com")
                     (replace-imgur (second (re-matches #"(.*\.com/.*)\..*" url)))
                     url))
         urls)))

(defn get-titles
  "For every url in a message, try to find a title."
  [dbconn urls]
  (filter #(not (nil? %)) (map (fn [url] (db/get-title dbconn url)) (imgur-filter urls))))

(defn title-msg [target title]
  (make-privmsg target title))

(defn parse-ctcp [msg]
  (let [text (msg :text)]
    (second (re-matches #"\u0001(.*?)\u0001" text))))

(defn ctcp-reply [target text]
  (notice target (str "\u0001" text "\u0001")))

(defn ctcp-ping-reply [target ping]
  (ctcp-reply target (str "PING " ping)))

(defn ctcp-version-reply [target version]
  (ctcp-reply target (str "VERSION " version)))

(defn is-cmd [privmsg]
  (= (subs (privmsg :text) 0 1) "!"))

(defn remove-colors
  "Remove mirc colors from message."
  [msg]
  (clojure.string/replace (msg :text) #"\p{C}((\d\d?)(,\d\d?)?)?" ""))

(defn handle-privmsg [dbconn user privmsg]
  (let [sender (privmsg :sender)
        target (let [t (privmsg :target)] (if (= t (user :nick)) sender t))
        ctcp-msg (parse-ctcp privmsg)]
    (log dbconn privmsg)
    (if (nil? ctcp-msg)
      (let [privmsg (assoc privmsg :text (remove-colors privmsg)) 
            urls (parse-urls privmsg)
            cmd (first (split-words (privmsg :text)))]
        (cond
          (= cmd "!urls") (for [url (urls-cmd dbconn privmsg)]            
                            (make-privmsg sender url))
          :else (for [title (get-titles dbconn urls)]
                  (title-msg target title))))
      (let [ping (re-matches #"PING (.+)" ctcp-msg)
            version (= ctcp-msg "VERSION")]
        (cond
          version (vector (ctcp-version-reply sender clientversion))
          ping (vector (ctcp-ping-reply sender (second ping)))
          :else [])))))


(defn msg-reply [msg server user dbconn]
  (case (msg :cmd)
    "PING" (vector (pong-msg (msg :params)))
    "001" (for [[channel key] (server :channels)]
            (join-msg channel key))
    "433" (let [rand-nick (str "bot" (rand-int 99999))]
            (dosync (alter user assoc :nick rand-nick))
            (vector (nick-msg @user)))
    "PRIVMSG" (let [privmsg (parse-privmsg msg)]
                (handle-privmsg dbconn @user privmsg))
    []))


(defn connect [server dbconn]
  (with-open [socket (Socket. (server :host) (server :port))
              out (socket-writer socket)
              in (socket-reader socket)
              send-msg (fn [msg]
                         (println (str "-> " msg))
                         (.print out (str msg "\r\n"))
                         (.flush out))
              user (ref (server :user))]
    (send-msg (nick-msg @user))
    (send-msg (user-msg @user))
    (loop []
      (if-let [server-msg (.readLine in)]
        (let [msg (parse-msg server-msg)]
          (doseq [reply (msg-reply msg server user dbconn)]
            (send-msg reply))
          (println (msg :text))
          (recur))))))
