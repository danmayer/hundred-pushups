(ns hundred-pushups.core
  (:require [clojure.spec :as s]))

;;;;;; specs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :exr/reps pos-int?)
(s/def :exr.pushup/reps :exr/reps)
(s/def :exr.plank/reps :exr/reps)
(s/def :exr/sets (s/int-in 4 11))

(s/def :exr/circuit
  (s/keys :req [:exr.pushup/reps :exr.plank/reps]))

(s/def :exr.circuit/log (s/coll-of :exr/circuit))
(s/def :exr/day
  (s/keys :req [:exr/sets :exr/circuit]))
(s/def :exr.circuit/test-log (s/coll-of :exr/circuit))

;;;;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn div-ceil [num den]
  (let [q (quot num den)
        r (rem num den)]
    (if (= 0 r)
      q
      (+ 1 q))))

(defn half [num]
  (div-ceil num 2))

(defn map-vals
  "Like `map`, but only for the values of a hash-map"
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

;;;;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef suggested-day
        :args (s/cat
               :test-log (s/and :exr.circuit/test-log
                                (fn [x] (pos? (count x))))
               :circuit-log :exr.circuit/log)
        :ret :exr/day)
(defn suggested-day [test-log circuit-log]
  (let [last-circuit (last circuit-log)
        last-test (last test-log)]
    {:exr/sets 4
     :exr/circuit
     (if last-circuit
       (map-vals inc last-circuit)
       (map-vals half last-test))}))
