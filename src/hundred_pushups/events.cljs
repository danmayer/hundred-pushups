(ns hundred-pushups.events
  (:require
    [clojure.set :as set]
    [cljs.core.async :as async]
    [clojure.data :as data]
    [clojure.spec :as s]
    [day8.re-frame.async-flow-fx :as async-flow-fx]
    [glittershark.core-async-storage :refer [get-item set-item]]
    [hundred-pushups.core :as core]
    [hundred-pushups.datetime :as dt]
    [hundred-pushups.db :as db :refer [default-db]]
    [re-frame.core :refer [reg-event-db after reg-event-fx dispatch reg-fx]]
    [re-frame.interceptor :refer [->interceptor get-effect get-coeffect assoc-coeffect assoc-effect]]
    [re-frame.loggers :refer [console]]
    ))

;; -- Helpers -----------------------------------------------------------------

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

;; -- Interceptors ------------------------------------------------------------

;; Copy of
;; https://github.com/Day8/re-frame/blob/797fc76006a447bc6553aac11ec720b5c304f97f/src/re_frame/std_interceptors.cljc#L16
;; but modified to print sensibly on React Native console
(def rn-debug
    "An interceptor which logs data about the handling of an event.
  Includes a `clojure.data/diff` of the db, before vs after, showing
  the changes caused by the event handler.
  You'd typically want this interceptor after (to the right of) any
  path interceptor.
  Warning:  calling clojure.data/diff on large, complex data structures
  can be slow. So, you won't want this interceptor present in production
  code. See the todomvc example to see how to exclude interceptors from
  production code."
  (->interceptor
   :id     :debug
   :before (fn debug-before
             [context]
             (console :log "Handling re-frame event:" (pr-str (get-coeffect context :event)))
             context)
   :after  (fn debug-after
             [context]
             (let [event   (get-coeffect context :event)
                   orig-db (get-coeffect context :db)
                   new-db  (get-effect   context :db ::not-found)]
               (if (= new-db ::not-found)
                 (console :log "No :db changes caused by:" (pr-str event))
                 (let [[only-before only-after] (data/diff orig-db new-db)
                       db-changed?    (or (some? only-before) (some? only-after))]
                   (if db-changed?
                     (do (console :group "db clojure.data/diff for:" (pr-str event))
                         (console :log "only before:" (pr-str only-before))
                         (console :log "only after :" (pr-str only-after))
                         (console :groupEnd))
                     (console :log "no app-db changes caused by:" (pr-str event)))))
                               context))))

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
 [validate-spec rn-debug]
 (fn [_world [_event-name db-from-local-storage]]
   {:dispatch [:db/init.ok]
    :db (merge default-db db-from-local-storage)}))

(reg-event-db
 :db/init.ok
 [validate-spec rn-debug]
 (fn [db [_event]]
   db))

(reg-event-db
 :db/reset
 [validate-spec rn-debug]
 (fn [_db [_event]]
   db/default-db))

(reg-event-db
 :complete-stage
 [validate-spec rn-debug]
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
 [validate-spec rn-debug]
 (fn [db [_event-name path val]]
   (update db :ui-state #(assoc-in % path val))))

(defn dbg [l x]
  (prn l x)
  x)

(reg-event-db
 :ui-state/clear
 [validate-spec rn-debug]
  (fn [db [_event-name paths]]
    (reduce
     (fn [d path]
       (update d :ui-state #(dissoc-in % path)))
     db
     paths)))

(reg-event-db
 :append-test
 [validate-spec rn-debug]
  (fn [db [_event-name test-circuit]]
    (update db :completed-test-log conj (assoc test-circuit :exr/ts (dt/now)))))

(reg-event-db
 :complete-day
 [validate-spec rn-debug]
 (fn [db [_event-name circuit ui-state]]
   (update db :completed-circuit-log into (core/merge-day-changes circuit ui-state (dt/now)))))

(reg-event-db
 :save-white-list
 [validate-spec rn-debug]
 (fn [db [_event-name day start end]]
   (update db :schedules #(assoc-in % [:white-list (keyword day)] [start end]))))

(reg-event-db
 :remove-from-whitelist
 [validate-spec rn-debug]
 (fn [db [_event-name day]]
   (update db :schedules #(dissoc-in % [:white-list (keyword day)]))))

(reg-event-db
 :select-tab
 [validate-spec rn-debug]
 (fn [db [_event-name idx]]
   (assoc db :selected-tab (get (set/map-invert db/tabs) idx))))

(reg-event-db
 :timer
 ;; Don't run debug middleware here, it'll be way too much output
 validate-spec
 (fn [db [_event-name new-time]]
   (assoc db :actual-time new-time)))

(reg-event-db
 :set-simulated-time
 [validate-spec rn-debug]
 (fn [db [_event-name simulated-time]]
   (assoc db :simulated-time (dt/moment-str->inst simulated-time))))
