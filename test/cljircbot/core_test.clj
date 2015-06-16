(ns cljircbot.core-test
  (:require [clojure.test :refer :all]
            [cljircbot.core :refer :all]))



;(deftest log-test
;  (let [msg {:text "foobar" :sender "foo" :target "bar"}]
;    (log-msg test-db msg)
;    (let [result (sql/query db ["SELECT * FROM url_msg"])))
