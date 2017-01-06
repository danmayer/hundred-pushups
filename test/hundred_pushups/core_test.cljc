(ns hundred-pushups.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [hundred-pushups.test-helper :refer :all]
            [hundred-pushups.core :refer :all]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]]))

(use-fixtures :once instrument-all check-asserts)

(defn complete-day [circuit-log day ts]
  (into circuit-log
        (day->log day ts)))

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
    (is (= {:exr/suggested-circuit
            {:exr/pushup-reps 5 :exr/plank-reps 8}
            :exr/sets 4}
           (suggested-day
            [{:exr/pushup-reps 10
              :exr/plank-reps 15
              :exr/ts dummy-ts}]
            []))))

  (testing "suggests 4 x 50% + 1 after one day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/suggested-circuit
              {:exr/pushup-reps 6 :exr/plank-reps 9}
              :exr/sets 4}
             (let [test-log [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts dummy-ts}]
                   circuit-log []]
               (->> circuit-log
                    (complete-next-day ts test-log)
                    (suggested-day test-log)))))))

  (testing "suggests 4 x 50% + 2 after two day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/suggested-circuit
              {:exr/pushup-reps 7 :exr/plank-reps 10}
              :exr/sets 4}
             (let [test-log [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts dummy-ts}]
                   circuit-log []]
               (->> circuit-log
                    (complete-next-day ts test-log)
                    (complete-next-day ts test-log)
                    (suggested-day test-log)))))))

  (testing "suggests a test if previous workout was less than suggested"
    (is (= :exr/do-test
           (suggested-day [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts (ts 0)}]
                          [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (ts 1)}]))))

  (testing "suggests reps if previous workout was less than suggested previously, but
            a more recent test has been completed"
    (is (= {:exr/suggested-circuit
            {:exr/pushup-reps 5 :exr/plank-reps 6}
              :exr/sets 4}
           (suggested-day [{:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (ts 0)}
                           {:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (ts 2)}]
                          [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (ts 1)}]))))

  (let [{args-sp :args ret-sp :ret} (s/get-spec #'suggested-day)]
    (checking
     "suggested circuit always has reps equal to or greater than last circuit reps (or requires a test)"
     10
     [[test-log circuit-log] (s/gen args-sp)]
     (let [day (suggested-day test-log circuit-log)]
       (when-not (= :exr/do-test day)
         (let [new-circ (:exr/completed-circuit day)
               last-circuit (last circuit-log)]
           (when (and new-circ last-circuit)
             (is (<= (:exr/pushup-reps last-circuit) (:exr/pushup-reps new-circ)))
             (is (<= (:exr/plank-reps last-circuit) (:exr/plank-reps new-circ))))))))))

(deftest local-date-test
  (testing "returns date based on timezone"
    (is (= [2016 01 01]
           (local-date #inst "2016-01-02T01:01:01Z")))
    (is (= [2016 01 02]
           (local-date #inst "2016-01-02T12:01:01Z")))))

(deftest last-days-log-test
  (testing "returns empty vector if there are no days"
    (is (= []
           (last-days-log []))))
  (testing "splits days"
    (is (= [{:exr/pushup-reps 1
             :exr/plank-reps 1
             :exr/ts #inst "2016-01-02T12:01:01Z"}]
           (last-days-log [{:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-01T12:01:01Z"}
                           {:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-02T12:01:01Z"}]))))
  (testing "groups days"
    (is (= [{:exr/pushup-reps 1
             :exr/plank-reps 1
             :exr/ts #inst "2016-01-02T12:01:01Z"}
            {:exr/pushup-reps 1
             :exr/plank-reps 1
             :exr/ts #inst "2016-01-02T13:01:01Z"}]
           (last-days-log [{:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-01T12:01:01Z"}
                           {:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-01T13:01:01Z"}
                           {:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-02T12:01:01Z"}
                           {:exr/pushup-reps 1
                            :exr/plank-reps 1
                            :exr/ts #inst "2016-01-02T13:01:01Z"}])))))

(deftest completed-circuit?-test
  (testing "returns false if completed too few sets"
    (is (= false
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 5 :exr/plank-reps 5}
                                :exr/sets 4}
                               []))))
  (testing "returns false if there are too few reps in any set"
    (is (= false
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 5 :exr/plank-reps 5}
                                :exr/sets 4}
                               [{:exr/pushup-reps 4 :exr/plank-reps 5}
                                {:exr/pushup-reps 4 :exr/plank-reps 5}
                                {:exr/pushup-reps 4 :exr/plank-reps 5}
                                {:exr/pushup-reps 5 :exr/plank-reps 5}])))
    (is (= false
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 5 :exr/plank-reps 5}
                                :exr/sets 4}
                               [{:exr/pushup-reps 5 :exr/plank-reps 5}
                                {:exr/pushup-reps 5 :exr/plank-reps 5}
                                {:exr/pushup-reps 5 :exr/plank-reps 5}
                                {:exr/pushup-reps 5 :exr/plank-reps 3}]))))
  (testing "returns true if completed all reps in all sets"
    (is (= true
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                :exr/sets 4}
                               [{:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}])))
    (is (= true
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                :exr/sets 4}
                               [{:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}]))))
  (testing "returns true if completed extra reps"
    (is (= true
           (completed-circuit? {:exr/suggested-circuit
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                :exr/sets 4}
                               [{:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 1}
                                {:exr/pushup-reps 1 :exr/plank-reps 2}])))))

(deftest parse-int-test
  (is (= 1 (parse-int "1")))
  (is (= 11 (parse-int "11"))))

(deftest merge-day-changes-test
  (is (= [{:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}]
         (merge-day-changes {:exr/suggested-circuit
                             {:exr/pushup-reps 1 :exr/plank-reps 1}
                             :exr/sets 4}
                            {:pushup-reps-text {}
                             :plank-reps-text {}}
                            dummy-ts)))

  (is (= [{:exr/pushup-reps 1 :exr/plank-reps 0 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}
          {:exr/pushup-reps 1 :exr/plank-reps 1 :exr/ts dummy-ts}]
         (merge-day-changes {:exr/suggested-circuit
                             {:exr/pushup-reps 1 :exr/plank-reps 1}
                             :exr/sets 4}
                            {:pushup-reps-text {}
                             :plank-reps-text {0 "0"}}
                            dummy-ts))))
