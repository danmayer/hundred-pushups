(ns hundred-pushups.db
  (:require [clojure.spec :as s]
            [hundred-pushups.core]))

;; spec of app-db
(s/def ::event keyword?)
(s/def ::completed-stages (s/coll-of ::event))
(s/def ::ui-state map?)
(s/def ::app-db
  (s/keys :req-un [::completed-stages
                   ::ui-state
                   :exr/completed-test-log
                   :exr/completed-circuit-log]))

;; initial state of app-db
(def default-db {:completed-stages []
                 :ui-state {}
                 :completed-test-log []
                 :completed-circuit-log []})
