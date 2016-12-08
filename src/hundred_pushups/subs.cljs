(ns hundred-pushups.subs
  (:require [re-frame.core :refer [reg-sub]]
            [hundred-pushups.core :as core]))

;; FIXME - delete after android no longer uses this
(reg-sub
  :get-greeting
  (fn [db _]
    (:greeting db)))

(reg-sub
 :stage
 (fn [db _]
   (case (last (:completed-stages db))
     nil :get-started
     :get-started :learn-pushup-form
     :learn-pushup-form :do-pushup-test
     :do-pushup-test :do-plank-test
     :do-plank-test :show-day
     :show-day :show-day)))

(reg-sub
 :ui-state/get
 (fn [db _]
   (:ui-state db)))

(reg-sub
 :db
 (fn [db _]
   db))

(reg-sub
 :days-exercise
 (fn [db _]
   (core/suggested-day (:test-log db)
                       (:circuit-log db))))
