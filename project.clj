(defproject cljircbot "0.1.0-SNAPSHOT"
  :description "IRC bot"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main cljircbot.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.1.2"]
                 [yesql "0.4.0"]
                 [slingshot "0.12.2"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [com.h2database/h2 "1.4.187"]
                 [enlive "1.1.5"]])
