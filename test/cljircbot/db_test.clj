(ns cljircbot.db-test
  (:require [cljircbot.db :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]))

(def dbspec (h2 "mem:")) 

(deftest tables-test
  (jdbc/with-db-connection [db dbspec]
    (create-tables db)
    (testing "tables are created"
      (let [rows (jdbc/query db ["SELECT * FROM msg"])]
        (is (not (nil? rows)))))))

(deftest add-test
  (jdbc/with-db-connection [db dbspec]
    (create-tables db)
    (add-msg db "foobar" "foo" "bar")
    (testing "msg is inserted"
      (let [row (first (jdbc/query db ["SELECT * FROM msg"]))]
        (is (= (row :target) "bar"))
        (is (= (row :sender) "foo"))
        (is (= (row :content) "foobar"))))
    (add-url db "foobar.com" "Foobar")
    (testing "url is inserted"
      (let [row (first (jdbc/query db ["SELECT * FROM url"]))]
        (is (= (row :address) "foobar.com"))
        (is (= (row :title) "Foobar"))))
    (add-url db "foobar.com" "Foobar")
    (testing "url is already in the db"
      (let [urls (jdbc/query db ["SELECT * FROM url"])
            relations (jdbc/query db ["SELECT * FROM url_msg WHERE url_msg.msgid = ?" 1])]
        (is (= (count relations) 2))
        (is (= (count urls) 1))))))


(deftest scrape-test
  (testing "get title from web page"
    (let [title "re-matches - clojure.core | ClojureDocs - Community-Powered Clojure Documentation and Examples"
          title2 "List of HTTP header fields - Wikipedia, the free encyclopedia"]
      (is (= nil (http-get-title "http://imgur.com/doesnotexist")))
      (is (= title2 (http-get-title "https://en.wikipedia.org/wiki/List_of_HTTP_header_fields")))
      (is (= nil (http-get-title "http://clojure.org/file/view/clojure-icon.gif")))
      (is (= title (http-get-title "https://clojuredocs.org/clojure.core/re-matches"))))))

(deftest get-test
  (jdbc/with-db-connection [db dbspec]
    (create-tables db)
    (add-msg db "foobar" "foo" "bar")
    (add-msg db "foobar" "foo" "bar")
    (add-msg db "foobar" "foo" "bar")
    (add-url db "foo1.com" "FooBar")
    (add-url db "foo2.com" "BarFoo")
    (add-url db "foo3.com" "BazQuux")
    (testing "find urls by target"
      (let [urls (get-urls db 5 "bar")
            urls2 (get-urls db 2 "bar")
            url (first urls)]
        (is (= "BazQuux" (:title url)))    
        (is (= "foo3.com" (:address url)))
        (is (= 3 (count urls)))
        (is (= 2 (count urls2)))))
    (testing "find urls by date"
      (let [urls (get-urls-by-date db "bar" (.toString (java.time.LocalDate/now)))]
        (is (= (count urls) 3))))
    (testing "find title by url"
      (is (= (get-title db "http://clojure.org") "Clojure - home"))
      (is (= (get-title db "foo1.com") "FooBar"))
      (is (= (get-title db "http://www.google.com") "Google")))))


            
