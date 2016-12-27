(ns hundred-pushups.core
  (:require
    [clojure.spec.test :as st]
    [clojure.spec :as s]

    #?@(:clj  [[clj-time.coerce :as time.coerce]
               [clj-time.core :as time]]
        :cljs [[cljs-time.coerce :as time.coerce]
               [cljs-time.core :as time]
               [cljs-time.format :as time.format]]))
  )

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

(s/def :exr/completed-circuits (s/keys :req [:exr/sets :exr/completed-circuits]))
(s/def :exr/suggested-circuits (s/keys :req [:exr/sets :exr/suggested-circuit]))

(s/def :exr/day
  (s/or
   :do-circuits :exr/suggested-circuits
   :do-test #{:exr/do-test}))

(s/def :exr/completed-circuit-log (s/coll-of :exr/completed-circuit))
(s/def :exr/completed-test-log (s/coll-of :exr/test))

;;;;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn now []
  (time.coerce/to-date (time/now)))

(def dummy-ts (time.coerce/to-date 0))

(defn local-date [inst]
  (let [dt (time.coerce/from-date inst)
        local-dt #?(:cljs (time/to-default-time-zone dt)
                    :clj (time/to-time-zone (time.coerce/to-date-time dt) (time/default-time-zone)))]
    [(time/year local-dt)
     (time/month local-dt)
     (time/day local-dt)]))

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
  (vec (last (partition-by (comp local-date :exr/ts) circuit-log))))

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
       (partition-by (comp local-date :exr/ts) )
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

;;;;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef suggested-day
        :args (s/cat
               :completed-test-log (s/and :exr/completed-test-log
                                          (fn [x] (pos? (count x))))
               :circuit-log :exr/completed-circuit-log)
        :ret :exr/day)
(defn suggested-day [completed-test-log circuit-log]
  (let [reps [:exr/pushup-reps :exr/plank-reps]
        last-circuit (select-keys (last circuit-log) reps)
        last-test (select-keys (last completed-test-log) reps)]
    (cond
      (empty? last-test)
      :exr/do-test

      (empty? last-circuit)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals half last-test)}

      (completed-circuit?
       (suggested-day completed-test-log (but-last-day circuit-log))
       circuit-log)
      {:exr/sets 4
       :exr/suggested-circuit (map-vals inc last-circuit)}

      :else
      :exr/do-test)))

(s/fdef complete-day
        :args (s/cat
               :circuit-log :exr/completed-circuit-log
               :day :exr/day
               :ts :exr/ts)
        :ret :exr/completed-circuit-log)
(defn complete-day [circuit-log day ts]
  (into circuit-log
        (day->log day ts)))
