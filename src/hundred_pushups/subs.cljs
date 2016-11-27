(ns hundred-pushups.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :get-greeting
  (fn [db _]
    (:greeting db)))

(reg-sub
 :last-stage
 (fn [db _]
   (last (:progress db))))
