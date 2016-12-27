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

(s/def :exr/zero-or-pos-int (s/or :pos pos-int?
                                  :zero zero?))
(s/def :exr/reps :exr/zero-or-pos-int)
(s/def :exr/pushup-reps :exr/reps)
(s/def :exr/plank-reps :exr/reps)
(s/def :exr/sets (s/int-in 4 11))
(s/def :exr/ts inst?)

(s/def :exr/circuit
  (s/keys :req [:exr/pushup-reps :exr/plank-reps :exr/ts]))
(s/def :exr/circuit-wo-ts
  (s/keys :req [:exr/pushup-reps :exr/plank-reps]
          :opt [:exr/ts]))
(s/def :exr/test :exr/circuit)

;; TODO - revisit the distinction between circuits and circuits without ts
(s/def :exr/circuits (s/keys :req [:exr/sets :exr/circuits]))
(s/def :exr/circuits-wo-ts (s/keys :req [:exr/sets :exr/circuit-wo-ts]))

(s/def :exr/day
  (s/or
   :do-circuits :exr/circuits-wo-ts
   :do-test #{:exr/do-test}))

(s/def :exr/circuit-log (s/coll-of :exr/circuit))
(s/def :exr/test-log (s/coll-of :exr/test))

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
;; TODO - not sure forming and unforming is helping at all here
(defn day->log [day ts]
  (let [[action circuits] (s/conform :exr/day day)]
    (case action
      :do-circuits
      (repeat (:exr/sets circuits)
              (assoc (s/unform :exr/circuit-wo-ts (:exr/circuit-wo-ts circuits))
                     :exr/ts ts))
      :do-test [])))

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
               :test-log (s/and :exr/test-log
                                (fn [x] (pos? (count x))))
               :circuit-log :exr/circuit-log)
        :ret :exr/day)
(defn suggested-day [test-log circuit-log]
  (let [reps [:exr/pushup-reps :exr/plank-reps]
        last-circuit (select-keys (last circuit-log) reps)
        last-test (select-keys (last test-log) reps)]
    (cond
      (empty? last-test)
      :exr/do-test

      (empty? last-circuit)
      {:exr/sets 4
       :exr/circuit-wo-ts (map-vals half last-test)}

      (completed-circuit?
         (suggested-day test-log (but-last-day circuit-log))
         circuit-log)
      {:exr/sets 4
       :exr/circuit-wo-ts (map-vals inc last-circuit)}

      :else
      :exr/do-test)))

(s/fdef complete-day
        :args (s/cat
               :circuit-log :exr/circuit-log
               :day :exr/day
               :ts :exr/ts)
        :ret :exr/circuit-log)
(defn complete-day [circuit-log day ts]
  (into circuit-log
        (day->log day ts)))
