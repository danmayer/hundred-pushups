(ns hundred-pushups.datetime-test
  (:require [clojure.test :refer [testing deftest is]]
            [clojure.spec :as s]
            #?@(:clj  [[clj-time.core :as time]
                       ]
                :cljs [[cljs-time.core :as time]])
            [hundred-pushups.datetime :refer [inst ct-fmt->moment-fmt now inst->str local-date parse-time]]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]))

(deftest now-test
  (is (inst? (now))))

(deftest inst-test
  (is (inst? (inst 0)))
  (is (inst? (inst 100000000000000))))

(deftest ct-fmt->moment-fmt-test
  (is (= "YYYY" (ct-fmt->moment-fmt "YYYY")))
  (is (= "DD" (ct-fmt->moment-fmt "dd"))))

(deftest inst->str-test
  (is (= "19700101T000000Z" (inst->str (inst 0))))
  (is (= "19700112T134640Z" (inst->str (inst 1000000000)))))


(deftest parse-time-test
  (is (= (inst (time/today-at 0 00)), (inst (parse-time "12am"))))
  (is (= (inst (time/today-at 9 00)), (inst (parse-time "9am"))))
  (is (= (inst (time/today-at 15 00)), (inst (parse-time  "3pm"))))
  (is (= (inst (time/today-at 23 00)), (inst (parse-time  "11pm")))))

;; FIXME - you can't set time zones in CLJS
;; https://github.com/andrewmcveigh/cljs-time/issues/14
;; so I'm not sure how to test this without breaking CI
#?(:clj
   (deftest local-date-test
     (testing "returns date based on timezone"
       (is (= 2016
              (first (local-date #inst "2016-01-02T01:01:01Z"))))
       (is (= [2016 01 01]
              (local-date #inst "2016-01-02T01:01:01Z" "America/Denver")))
       (is (= [2016 01 02]
              (local-date #inst "2016-01-02T12:01:01Z" "America/Denver"))))))
