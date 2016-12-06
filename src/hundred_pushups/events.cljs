(ns hundred-pushups.events
  (:require
    [clojure.spec :as s]
    [day8.re-frame.async-flow-fx :as async-flow-fx]
    [glittershark.core-async-storage :refer [get-item set-item]]
    [hundred-pushups.db :as db :refer [default-db]]
    [re-frame.core :refer [reg-event-db after reg-event-fx dispatch reg-fx]]
    [cljs.core.async :as async]
    [hundred-pushups.core :as core]
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
 validate-spec
 (fn [db [_event]]
   db))

(reg-event-db
 :db/reset
 validate-spec
 (fn [_db [_event]]
   db/default-db))

(reg-event-db
 :complete-stage
 validate-spec
 (fn [db [_event-name stage]]
   (update db :completed-stages conj stage)))

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

(reg-event-db
 :ui-state/set
  validate-spec
 (fn [db [_event-name path val]]
   (update db :ui-state #(assoc-in % path val))))

;; https://github.com/weavejester/medley/blob/master/src/medley/core.cljc#L11
(defn dissoc-in
    "Dissociate a value in a nested assocative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures."
  [m ks]
  (if-let [[k & ks] (seq ks)]
    (if (seq ks)
      (let [v (dissoc-in (get m k) ks)]
        (if (empty? v)
          (dissoc m k)
          (assoc m k v)))
      (dissoc m k))
        m))

(reg-event-db
 :ui-state/clear
  validate-spec
  (fn [db [_event-name paths]]
    (reduce
     (fn [d path]
       (update db :ui-state #(dissoc-in % path))
       )
     db
     paths)))

(reg-event-db
 :append-test
 validate-spec
  (fn [db [_event-name test-circuit]]
    (update db :test-log conj test-circuit)))

(reg-event-db
 :complete-day
 validate-spec
 (fn [db [_event-name day-schedule]]
   (update db :log core/complete-day day-schedule (core/now))))
