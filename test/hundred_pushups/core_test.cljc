(ns hundred-pushups.core-test
  (:require [clojure.test :refer [testing use-fixtures is deftest]]
            [clojure.spec :as s]
            [clojure.test.check.generators :as gen]
            #?@(:clj  [[clj-time.core :as time]
                       ]
                :cljs [[cljs-time.core :as time]])
            [hundred-pushups.test-helper :refer [instrument-all check-asserts] :include-macros true]
            [hundred-pushups.core :refer [day->log suggested-day analyze-history dummy-ts last-days-log completed-circuit? parse-int merge-day-changes format-whitelist-row valid-hour-time todays-range next-workout-time]]
            [hundred-pushups.datetime :as dt]
            [com.gfredericks.test.chuck.clojure-test :as ct :refer [checking]])
  #?(:cljs
     (:require-macros hundred-pushups.test-helper)))

(use-fixtures :once instrument-all check-asserts)

(s/fdef complete-day
        :args (s/cat
               :history :exr/history
               :circuits :exr/suggested-circuits
               :ts :exr/ts))
(defn complete-day [history suggested-circuits ts]
  (update history :exr/circuits into (day->log suggested-circuits ts)))

(defn complete-next-day [history ts]
  (let [next-day (suggested-day history)]
    (complete-day history next-day ts)))

#?(:clj
   ;; FIXME - for some reason, this does not run correctly in cljs
   ;; tests. I can't get the implementation for conforms-to? to load
   (deftest suggested-day-spec
     (let [{args-sp :args ret-sp :ret} (s/get-spec #'suggested-day)]
       (checking
        "conforms to spec"
        20
        [args (s/gen args-sp)]
        (is (conforms-to? ret-sp (apply suggested-day args)))))))

(deftest analyze-history-test
  (testing "after initial test"
    (is (=
         {:last-workout-completed? false
          :fresh-test? true}
         (-> (analyze-history
              {:exr/tests
               [{:exr/pushup-reps 10
                 :exr/plank-reps 15
                 :exr/ts dummy-ts}]
               :exr/circuits []})
             (select-keys [:last-workout-completed? :fresh-test?])
             ))))
  (testing "after completing first day"
    (let [ts #inst "2016-01-01"]
      (is (=
           {:last-workout-completed? true
            :fresh-test? false}
           (-> {:exr/tests
                [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts dummy-ts}]
                :exr/circuits
                []}
               (complete-next-day ts)
               (analyze-history)
               (select-keys [:last-workout-completed? :fresh-test?]))))))

  (testing "previous workout was less than suggested"
    (is (= {:last-workout-completed? false
            :fresh-test? false}
           (-> (analyze-history
                {:exr/tests
                 [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts (dt/inst 0)}]
                 :exr/circuits
                 [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (dt/inst 1)}]})
               (select-keys [:last-workout-completed? :fresh-test?])
               ))))

  (testing "previous workout was less than suggested previously, but
            a more recent test has been completed"
    (is (= {:last-workout-completed? false
            :fresh-test? true}
           (-> (analyze-history
                {:exr/tests
                 [{:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (dt/inst 0)}
                  {:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (dt/inst 2)}]
                 :exr/circuits
                 [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (dt/inst 1)}]})
               (select-keys [:last-workout-completed? :fresh-test?]))))))

(deftest suggesting-sets-and-reps
  (testing "suggests 4 x 50% reps (rounding up) after initial test"
    (is (= {:exr/suggested-circuit
            {:exr/pushup-reps 5 :exr/plank-reps 8}
            :exr/sets 4}
           (suggested-day
            {:exr/tests
             [{:exr/pushup-reps 10
               :exr/plank-reps 15
               :exr/ts dummy-ts}]
             :exr/circuits []}))))

  (testing "suggests 4 x 50% + 1 after one day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/suggested-circuit
              {:exr/pushup-reps 6 :exr/plank-reps 9}
              :exr/sets 4}
             (-> {:exr/tests
                  [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts dummy-ts}]
                  :exr/circuits
                  []}
                 (complete-next-day ts)
                 (suggested-day))))))

  (testing "suggests 4 x 50% + 2 after two day"
    (let [ts #inst "2016-01-01"]
      (is (= {:exr/suggested-circuit
              {:exr/pushup-reps 7 :exr/plank-reps 10}
              :exr/sets 4}
             (-> {:exr/tests
                  [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts dummy-ts}]
                  :exr/circuits
                  []}
                 (complete-next-day ts)
                 (complete-next-day ts)
                 (suggested-day))))))

  (testing "suggests a test if previous workout was less than suggested"
    (is (= :exr/do-test
           (suggested-day
            {:exr/tests
             [{:exr/pushup-reps 10 :exr/plank-reps 15 :exr/ts (dt/inst 0)}]
             :exr/circuits
             [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (dt/inst 1)}]}))))

  (testing "suggests reps if previous workout was less than suggested previously, but
            a more recent test has been completed"
    (is (= {:exr/suggested-circuit
            {:exr/pushup-reps 5 :exr/plank-reps 6}
            :exr/sets 4}
           (suggested-day
            {:exr/tests
             [{:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (dt/inst 0)}
              {:exr/pushup-reps 10 :exr/plank-reps 12 :exr/ts (dt/inst 2)}]
             :exr/circuits
             [{:exr/pushup-reps 0 :exr/plank-reps 0 :exr/ts (dt/inst 1)}]}))))

  ;; FIXME - I'm getting weird warnings around this on node tests. Maybe problems importing the macro?
  #?(:clj
     (let [{args-sp :args ret-sp :ret} (s/get-spec #'suggested-day)]
       (checking
        "suggested circuit always has reps equal to or greater than last circuit reps (or requires a test)"
        10
        [args (s/gen args-sp)]
        (let [[history] args
              {:keys [:exr/circuits :exr/tests]} history
              day (suggested-day history)]
          (when-not (= :exr/do-test day)
            (let [new-circ (:exr/circuit day)
                  last-circuit (last circuits)]
              (when (and new-circ last-circuit)
                (is (<= (:exr/pushup-reps last-circuit) (:exr/pushup-reps new-circ)))
                (is (<= (:exr/plank-reps last-circuit) (:exr/plank-reps new-circ)))))))))))

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

(deftest format-whitelist-row-test
  (is (= "Monday: 9AM-5PM", (format-whitelist-row ["Monday" ["9AM" "5PM"]])))
  (is (= "Monday: 9AM-5PM", (format-whitelist-row [:Monday ["9AM" "5PM"]]))))

(deftest valid-hour-time-test
  (is (= false (valid-hour-time "9not")))
  (is (= false (valid-hour-time  "9AM")))
  (is (= false (valid-hour-time  "")))
  (is (= false (valid-hour-time  nil)))
  (is (= true  (valid-hour-time  "9am")))
  (is (= false (valid-hour-time "abc")))
  (is (= false (valid-hour-time "123")))
  (is (= false (valid-hour-time "abc") ))
  (is (= true  (valid-hour-time "5pm")))
  (is (= true  (valid-hour-time "9am") ))
  (is (= true  (valid-hour-time "5pm"))))

(deftest todays-range-test
  (is (= [(time/today-at 9 00) (time/today-at 17 00)], (todays-range {:monday ["9am" "5pm"]
                                                                      :tuesday ["9am" "5pm"]
                                                                      :wednesday ["9am" "5pm"]
                                                                      :thursday ["9am" "5pm"]
                                                                      :friday ["9am" "5pm"]
                                                                      :saturday ["9am" "5pm"]
                                                                      :sunday ["9am" "5pm"]}
                                                                     []))))

;; nil nothing left to do
;; or you are done
;; pass in current-time
;; should we auto-black list next hour
(deftest next-workout-time-test
  (let [ts-range (todays-range {:monday ["9am" "5pm"]
                                :tuesday ["9am" "5pm"]
                                :wednesday ["9am" "5pm"]
                                :thursday ["9am" "5pm"]
                                :friday ["9am" "5pm"]
                                :saturday ["9am" "5pm"]
                                :sunday ["9am" "5pm"]}
                               [])]
    (is (= (time/today-at 11 00) (next-workout-time {:current-time (time/today-at 11 00) :num-sets 4 :available-ranges ts-range})))
    (is (= nil (next-workout-time {:current-time (time/today-at 11 00) :num-sets 0 :available-ranges ts-range})))
    (is (= (time/today-at 9 00) (next-workout-time {:current-time (time/today-at 6 00) :num-sets 4 :available-ranges ts-range})))
    (is (= nil (next-workout-time {:current-time (time/today-at 19 00) :num-sets 4 :available-ranges ts-range})))
    ))
