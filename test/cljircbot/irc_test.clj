(ns cljircbot.irc-test
  (:require [cljircbot.irc :refer :all]
            [cljircbot.db :refer :all]
            [clojure.test :refer :all]))

(deftest parse-test
  (testing "parse urls"
    (is (is-url "https://foo.com"))
    (is (is-url "http://www.bar.com/foo?baz=quux"))
    (is (not (is-url "foo.com")))
    (let [urls (parse-urls {:text "http://foo.com http://bar.com foo"})]
      (is (= (count urls) 2))
      (is (= (first urls) "http://foo.com"))
      (is (= (second urls) "http://bar.com"))))
  (testing "parse message"
    (let [line ":nick!user@host.foo PRIVMSG #channelname :message"
          msg (parse-msg line)
          privmsg (parse-privmsg msg)]
      (is (= (msg :cmd) "PRIVMSG"))
      (is (= (msg :prefix) "nick!user@host.foo"))
      (is (= (privmsg :text) "message"))
      (is (= (privmsg :sender) "nick"))
      (is (= (privmsg :target) "#channelname")))))

(deftest imgur-test
  (testing "transform imgur url"
    (let [images ["http://i.imgur.com/image.gif" "http://google.com"]]
      (is (= (second (imgur-filter images)) "http://google.com"))
      (is (= (first (imgur-filter images)) "http://imgur.com/image")))))
