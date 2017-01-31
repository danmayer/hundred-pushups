(ns hundred-pushups.core
  (:require
    [clojure.spec.test :as st]
    [clojure.spec :as s]
    [hundred-pushups.datetime :as dt]))

;;;;;; specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :exr/zero-or-pos-int (s/and int?
                                   #(<= 0 %)))
(s/def :exr/reps :exr/zero-or-pos-int)
(s/def :exr/pushup-reps :exr/reps)
(s/def :exr/plank-reps :exr/reps)
(s/def :exr/sets (s/int-in 4 20))
(s/def :exr/ts inst?)

(s/def :exr/completed-circuit
  (s/keys :req [:exr/pushup-reps :exr/plank-reps :exr/ts]))
(s/def :exr/suggested-circuit
  (s/keys :req [:exr/pushup-reps :exr/plank-reps]))
(s/def :exr/completed-test
  (s/keys :req [:exr/pushup-reps :exr/plank-reps :exr/ts]))

(s/def :exr/suggested-circuits (s/keys :req [:exr/sets :exr/suggested-circuit]))

;; TODO - maybe rename?
(s/def :exr/day
  (s/or
   :do-circuits :exr/suggested-circuits
   :do-test #{:exr/do-test}))

;; TODO - rename to just "completed-circuits"
(s/def :exr/completed-circuit-log (s/coll-of :exr/completed-circuit))
(s/def :exr/completed-test-log (s/coll-of :exr/completed-test))
(s/def :exr/history (s/and (s/keys :req [:exr/completed-circuit-log
                                         :exr/completed-test-log])
                           #(pos?
                             (count (:exr/completed-test-log %)))))

;;;;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dummy-ts (dt/inst 0))

(defn parse-int [x]
  #?(:clj  (Integer/parseInt x)
     :cljs (js/parseInt x)))

(defn div-ceil [num den]
  (let [q (quot num den)
        r (rem num den)]
    (if (= 0 r)
      q
      (+ 1 q))))

(defn half [num]
  (div-ceil num 2))

(defn map-vals
  "Like `map`, but only for the values of a hash-map that pass the key predicate"
  [f m]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(defn last-days-log [circuit-log]
  (vec (last (partition-by (comp dt/local-date :exr/ts) circuit-log))))

(s/fdef day->log
        :args (s/cat :day :exr/day
                     :ts :exr/ts))
(defn day->log [day ts]
  (if (= day :exr/do-test)
    []
    (repeat (:exr/sets day)
            (assoc (:exr/suggested-circuit day)
                   :exr/ts ts))))

(defn but-last-day [circuit-log]
  (->> circuit-log
       (partition-by (comp dt/local-date :exr/ts) )
       butlast
       flatten
       vec))

(defn complete? [expected-circuit actual-circuit]
  (and (<= (:exr/pushup-reps expected-circuit)
           (:exr/pushup-reps actual-circuit))
       (<= (:exr/plank-reps expected-circuit)
           (:exr/plank-reps actual-circuit))))

(defn completed-circuit? [expected-day actual-log]
  (let [expected-log (day->log expected-day dummy-ts)]
    (and
     (every? true? (map complete? expected-log actual-log))
     (<= (count expected-log) (count actual-log)))))

(defn ts-greater? [ts1 ts2]
  ;; <= works for instants in CLJS, but not CLJ
  (neg? (compare ts1 ts2)))

;; TODO - pull into util
(defn dbg [l x ]
  (prn l x)
  x)

;;;;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO - delete
(s/fdef suggested-day
        :args (s/cat
               :completed-test-log (s/and :exr/completed-test-log
                                          (fn [x] (pos? (count x))))
               :circuit-log :exr/completed-circuit-log)
        :ret :exr/day)
(defn suggested-day [completed-test-log circuit-log]
  (let [last-circuit (last circuit-log)
        last-test (last completed-test-log)]
    (cond
      (nil? last-test)
      :exr/do-test

      (nil? last-circuit)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals half (dissoc last-test :exr/ts))}

      (ts-greater? (:exr/ts last-circuit (dt/inst 0)) (:exr/ts last-test))
      {:exr/sets 4
       :exr/suggested-circuit (map-vals half (dissoc last-test :exr/ts))}

      (completed-circuit?
       (suggested-day completed-test-log (but-last-day circuit-log))
       circuit-log)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals inc (dissoc last-circuit :exr/ts))}

      :else
      :exr/do-test)))

;; TODO - rename
(s/fdef suggested-day1
        :args (s/cat
               :history :exr/history)
        :ret :exr/day)
(defn suggested-day1 [history]
  (let [{:keys [:exr/completed-circuit-log
                :exr/completed-test-log]} history
        last-circuit (last completed-circuit-log) 
        last-test (last completed-test-log)]
    (cond
      (nil? last-test)
      :exr/do-test

      (nil? last-circuit)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals half (dissoc last-test :exr/ts))}

      (ts-greater? (:exr/ts last-circuit (dt/inst 0)) (:exr/ts last-test))
      {:exr/sets 4
       :exr/suggested-circuit (map-vals half (dissoc last-test :exr/ts))}

      (completed-circuit?
       (suggested-day completed-test-log (but-last-day completed-circuit-log))
       completed-circuit-log)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals inc (dissoc last-circuit :exr/ts))}

      :else
      :exr/do-test)))



(defn ui-state->path [ui-state]
  (concat
   (for [[k v]  (into [] (:pushup-reps-text ui-state))]
     [[k :exr/pushup-reps] v])
   (for [[k v]  (into [] (:plank-reps-text ui-state))]
     [[k :exr/plank-reps] v] )))

(defn merge-day-changes [day ui-state ts]
  (reduce
   (fn [log [path v]]
     (assoc-in
      log
      path
      (parse-int v)))
   (vec (day->log day ts))
   (ui-state->path ui-state)))

(defn format-whitelist-row [row]
  (str (name(first row)) ": " (clojure.string/join "-" (last row))))
