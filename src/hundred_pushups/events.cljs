(ns hundred-pushups.events
  (:require
    [clojure.spec :as s]
    [day8.re-frame.async-flow-fx :as async-flow-fx]
    [glittershark.core-async-storage :refer [get-item set-item]]
    [hundred-pushups.db :as db :refer [default-db]]
    [re-frame.core :refer [reg-event-db after reg-event-fx dispatch reg-fx]]
    [cljs.core.async :as async]
    ))

;; -- Interceptors ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

;; -- Boot --------------------------------------------------------------

(defn boot-flow []
  {:first-dispatch [:db/load]
   :rules
   [{:when :seen? :events [:db/init.ok] :halt? true}]})

;; -- Handlers --------------------------------------------------------------

(def local-storage-db-key :db)

(reg-event-fx
 :boot/init
 (fn [_ _]
   {:async-flow (boot-flow)}))

(reg-event-fx
 :db/load
 (fn [{db :db} [_event-name val]]
   {:db/load {}}))

(reg-fx
 :db/load
 (fn load-db-effect [_m]
   (async/take! (get-item local-storage-db-key)
                (fn [[error value]]
                  (if error
                    (dispatch [:db/load.err error])
                    (dispatch [:db/load.ok value]))))))

(reg-event-fx
 :db/load.ok
 validate-spec
 (fn [_world [_event-name db-from-local-storage]]
   {:dispatch [:db/init.ok]
    :db (or db-from-local-storage default-db)}))

(reg-event-db
 :db/init.ok
 (fn [db [_event]]
   db))

(reg-event-db
 :set-greeting
 validate-spec
 (fn [db [_ value]]
   (assoc db :greeting value)))

(reg-event-db
 :more-greeting
 validate-spec
 (fn [db [_event-name _value]]
   (update db :greeting #(str % "!"))))

(reg-fx
 :db/save
 (fn save-db-effect [{db :db}]
   (async/take! (set-item local-storage-db-key db)
                (fn [[error value]]
                  (if error
                    (dispatch [:db/save.err error])
                    (dispatch [:db/save.ok]))))))

(reg-event-fx
 :db/save.er
 (fn [_world event]
   (println event)
   {}))

(reg-event-fx
 :db/save.ok
 (fn [_world event]
   (println event)))

(reg-event-fx
 :db/save
 (fn [{db :db} [_event-name _value]]
   {:db/save {:db db}}))
