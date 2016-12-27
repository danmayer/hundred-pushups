(ns hundred-pushups.db-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [hundred-pushups.test-helper :refer :all]
            [hundred-pushups.db :refer :all :as db]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]))

(deftest default-db-test
  (is (= "Success!\n"
         (s/explain-str ::db/app-db default-db))))
