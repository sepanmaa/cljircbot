(ns cljircbot.db
  (:gen-class)
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as http]
            [yesql.core :refer [defqueries]]))

(defn h2 [file]
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname file})

(defqueries "sql/tables.sql")
(defqueries "sql/queries.sql")

(defn create-tables [db]
  (create-table-msg! db)
  (create-table-url! db)
  (create-table-url-msg! db))

(defn add-msg [db content sender target]
  (try (insert-msg! db content sender target)
       (:id (first (find-last-msgid db)))
       (catch Exception e (println (.getMessage e)))))

(defn add-url [db address title]
  (try
    (let [msgid (:id (first (find-last-msgid db)))]
      (if (= (count (find-urls-by-address db address)) 0)
        (insert-url! db address title))
      (let [urlid (:id (first (find-urlid-by-address db address)))]
        (insert-url-msg! db urlid msgid)
        urlid))
    (catch Exception e (println (.getMessage e)))))

(defn get-urls [db num target]  
  (take num (find-urls-by-target db target (.toString (java.time.LocalDate/now)))))

(defn get-urls-by-date [db target date]
  (try+ (let [parsed-date (.toString (java.time.LocalDate/parse date))]
         (find-urls-by-target db target parsed-date))
        (catch java.time.DateTimeException e
          (println (.getMessage e)) [])
        (catch Object _ [])))

(defn http-get-title [url]
  (try+
    (let [res (http/get url)
          content-type (get (:headers res) "content-type")]
      (if (.contains content-type "text/html")
        (let [res-body (java.io.StringReader. (:body res))
              selected (html/select (html/html-resource res-body) [:title])]
          (if (= (count selected) 0)
            "no title"
            (let [title (clojure.string/trim (first (:content (first selected))))]
              (if (> (count title) 512)
                (subs title 0 512)
                title))))))
    (catch [:status 404] {:keys [request-time headers body]}
      (println (str url ": 404 Not Found")))
    (catch javax.net.ssl.SSLException e
      (if (re-find (re-matcher #"https:" url))
        (http-get-title (clojure.string/replace url #"https:" "http:"))
        (println (.getMessage e))))
    (catch Object _
      (println "Unexpected error."))))
                         

(defn get-title [db url]
  (let [titles (find-titles-by-address db url)]
    (if (= (count titles) 0)
      (let [title (http-get-title url)]
        (if (not (nil? title))
          (add-url db url title))
        title)
      (:title (first titles)))))
    
      
