(ns hundred-pushups.db
  (:require [clojure.spec :as s]
            [hundred-pushups.core]))

;; spec of app-db
(s/def ::event keyword?)
(s/def ::completed-stages (s/coll-of ::event))
(s/def ::ui-state map?)
(s/def ::schedules map?)
(s/def ::app-db
  (s/keys :req-un [::completed-stages
                   ::ui-state
                   :exr/completed-test-log
                   :exr/completed-circuit-log
                   ::schedules]))

;; initial state of app-db
(def default-db {:completed-stages []
                 :ui-state {:schedule-day-text "monday"}
                 :schedules {:white-list {:monday ["9am" "5pm"]
                                          :tuesday ["9am" "5pm"]}
                             :black-list []}
                 :test-log []
                 :completed-test-log []
                 :completed-circuit-log []})
