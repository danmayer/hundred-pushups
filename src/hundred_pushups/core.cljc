(ns hundred-pushups.core
  (:require [clojure.spec :as s]))

;;;;;; specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :exr/reps pos-int?)
(s/def :exr/day
  (s/keys :req [:exr/sets :exr/circuit]))
(s/def :exr/ts inst?)

(s/def :exr/pushup-reps :exr/reps)
(s/def :exr/plank-reps :exr/reps)
(s/def :exr/sets (s/int-in 4 11))

(s/def :exr/circuit
  (s/keys :req [:exr/pushup-reps :exr/plank-reps]
          :opt [:exr/ts]))
(s/def :exr/test :exr/circuit)
(s/def :exr/circuit-log (s/coll-of :exr/circuit))
(s/def :exr/test-log (s/coll-of :exr/test))

;;;;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn now []
  #?(:clj (java.util.Date.)
     :cljs (js/Date.)))

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
    {:exr/sets 4
     :exr/circuit
     (if-not (empty? last-circuit)
       (map-vals inc last-circuit)
       (map-vals half last-test))}))

(s/fdef complete-day
        :args (s/cat
               :circuit-log :exr/circuit-log
               :day :exr/day
               :ts :exr/ts)
        :ret :exr/circuit-log)
(defn complete-day [circuit-log day ts]
  (into circuit-log
         (repeat (:exr/sets day)
                 (assoc (:exr/circuit day)
                        :exr/ts ts))))
