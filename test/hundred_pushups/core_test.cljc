(ns hundred-pushups.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [hundred-pushups.test-helper :refer :all]
            [hundred-pushups.core :refer :all]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]))

(use-fixtures :once instrument-all check-asserts)

(defn complete-next-day [ts test-log log]
  (let [next-day (suggested-day test-log log)]
    (complete-day log next-day ts)))

(deftest now-test
  (is (inst? (now))))

(deftest suggested-day-spec
  (let [{args-sp :args ret-sp :ret} (s/get-spec #'suggested-day)]
    (checking
     "conforms to spec"
     20
     [args (s/gen args-sp)]
     (is (conforms-to? ret-sp (apply suggested-day args))))))

(deftest suggesting-sets-and-reps
  (testing "suggests 4 x 50% reps (rounding up) after initial test"
    (is (= {:exr/circuit
            {:exr.pushup/reps 5 :exr.plank/reps 8}
            :exr/sets 4}
           (suggested-day
            [{:exr.pushup/reps 10
              :exr.plank/reps 15}]
            []))))

  (testing "suggests 4 x 50% + 1 after one day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/circuit
              {:exr.pushup/reps 6 :exr.plank/reps 9}
              :exr/sets 4}
             (let [test-log [{:exr.pushup/reps 10 :exr.plank/reps 15}]
                   circuit-log []]
               (->> circuit-log
                    (complete-next-day ts test-log)
                    (suggested-day test-log)))))))

  (testing "suggests 4 x 50% + 2 after two day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/circuit
              {:exr.pushup/reps 7 :exr.plank/reps 10}
              :exr/sets 4}
             (let [test-log [{:exr.pushup/reps 10 :exr.plank/reps 15}]
                   circuit-log []]
               (->> circuit-log
                    (complete-next-day ts test-log)
                    (complete-next-day ts test-log)
                    (suggested-day test-log)))))))

  (let [{args-sp :args ret-sp :ret} (s/get-spec #'suggested-day)]
    (checking
     "suggested circuit is always has reps equal to or greater than last circuit reps"
     10
     [[test-log circuit-log] (s/gen args-sp)]
     (let [new-circ (:exr/circuit (suggested-day test-log circuit-log))
           last-circuit (last circuit-log)]
       (when (and new-circ last-circuit)
         (is (<= (:exr.pushup/reps last-circuit) (:exr.pushup/reps new-circ)))
         (is (<= (:exr.plank/reps last-circuit) (:exr.plank/reps new-circ))))))))

(deftest complete-day-spec
  (let [{args-sp :args ret-sp :ret} (s/get-spec #'complete-day)]
    (checking
     "conforms to spec"
     20
     [args (s/gen args-sp)]
     (is (conforms-to? ret-sp (apply complete-day args))))))
