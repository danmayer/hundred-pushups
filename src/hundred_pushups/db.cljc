(ns hundred-pushups.db
  (:require [clojure.spec :as s]
            [hundred-pushups.core]))

;; spec of app-db
(s/def ::event keyword?)
(s/def ::completed-stages (s/coll-of ::event))
(s/def ::ui-state map?)
(s/def ::schedules map?)
(s/def ::selected-tab #{:set-schedule :work-out :dev})
(s/def ::actual-time inst?)
(s/def ::simulated-time (s/nilable inst?))
(s/def ::app-db
  (s/keys :req-un [::completed-stages
                   ::schedules
                   ::ui-state
                   :exr/completed-circuit-log
                   :exr/completed-test-log
                   ::selected-tab
                   ::actual-time
                   ::simulated-time
                   ]))

(def tabs {:set-schedule 0
           :work-out 1
           :dev 2})

;; initial state of app-db
(def default-db {:completed-stages []
                 :selected-tab :work-out
                 :ui-state {:schedule-day-text "monday"}
                 :schedules {:white-list {:monday ["9am" "5pm"]
                                          :tuesday ["9am" "5pm"]}
                             :black-list []}
                 :test-log []
                 :completed-test-log []
                 :completed-circuit-log []
                 :actual-time #inst "2017-01-15T23:49:36Z"
                 :simulated-time nil})
