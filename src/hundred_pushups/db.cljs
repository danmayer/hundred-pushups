(ns hundred-pushups.db
  (:require [clojure.spec :as s]
            [hundred-pushups.core]))

;; spec of app-db
(s/def ::event keyword?)
(s/def ::completed-stages (s/coll-of ::event))
(s/def ::ui-state map?)
(s/def ::ui-mode map?)
(s/def ::app-db
  (s/keys :req-un [::completed-stages
                   ::ui-state
                   ::ui-mode
                   :exr/test-log
                   :exr/circuit-log]))

;; initial state of app-db
(def default-db {:completed-stages []
                 :ui-state {}
                 :test-log []
                 :circuit-log []
                 :ui-mode {}})
