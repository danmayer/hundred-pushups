(ns hundred-pushups.db
  (:require [clojure.spec :as s]))

;; spec of app-db
(s/def ::event keyword?)
(s/def ::progress (s/coll-of ::event))
(s/def ::app-db
  (s/keys :req-un [::progress]))

;; initial state of app-db
(def default-db {:progress []})
