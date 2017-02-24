(ns hundred-pushups.datetime
  "Functions to convert between three datetime formats.

  cljs-time/clj-time have internal time formats that
  are useful for doing datetime changes, formatting, and conversions.

  Our app state uses instants (which are js/Date objects in CLJS and
  java.util.Date objects on CLJ). These are easy to readn and write
  from Clojure(script).

  Finally, a React Native component (react-native-datepicker) uses
  moment.js formats."

  (:require [clojure.spec :as s]
            [clojure.string :as str]
            #?@(:clj  [[clj-time.coerce :as time.coerce]
                       [clj-time.core :as time]
                       [clj-time.format :as time.format]]
                :cljs [[cljs-time.coerce :as time.coerce]
                       [cljs-time.core :as time]
                       [cljs-time.format :as time.format]
                       [cljsjs.moment :as moment]])))

(defn now
  "The current datetime"
  []
  (time.coerce/to-date (time/now)))

(defn inst
  "Given seconds from epoch, returns an inst"
  [second-since-epoch]
  (time.coerce/to-date second-since-epoch))

(defn ct-fmt->moment-fmt
  "Converts a cljs-time format string to a moment.js format str
   See http://momentjs.com/docs/#/parsing/string-format/ for details."
  [ct-fmt]
  (-> (s/assert some? ct-fmt)
      (str/replace "yyyy" "YYYY")
      (str/replace "dd" "DD")))

(defn ct-formatter []
  (:basic-date-time-no-ms time.format/formatters))


;; format time in hours only
(def day-of-formatter (time.format/formatter "ha"))

(defn parse-time [time-string]
  "parse hours to time today"
  (time/today-at (time/hour (time.format/parse day-of-formatter time-string)) 00))

(defn day-symbol [day-num]
  (keyword  (["monday" "tuesday" "wednesday" "thursday" "friday" "saturday" "sunday"] (dec day-num))))

(defn day-week-symbol []
  (day-symbol (time/day-of-week (time/today))))

(defn today []
  (time/today))

(defn in-time-interval? [now-time first last]
  (time/within? (time/interval first last)
              now-time))

(defn before? [time-one time-two]
  (time/before? time-one time-two))

#?(:cljs
   (defn moment-fmt []
     (ct-fmt->moment-fmt (:format-str (ct-formatter)))))

#?(:cljs
   (defn moment-str->inst
     "Given a string, returns an inst"
     [moment-str]
     (if-not moment-str
       nil
       (let [time-str (-> moment-str
                          (js/moment (moment-fmt))
                          (.utc)
                          (.format (moment-fmt))
                          (str/replace "+00:00" "Z")
                          (str/replace "'T'" "T"))]
         (time.coerce/to-date (time.format/parse (ct-formatter) time-str))))))

(defn inst->str
  "Given an inst, returns a string representation
  using the default formatter."
  [inst]
  (time.format/unparse (ct-formatter) (time.coerce/from-date inst)))

(defn local-date
  "Given an inst, returns a [local-date local-month local-day]"
  ([inst]
   (local-date inst :default))
  ([inst zone]
   (let [dt (time.coerce/from-date inst)
         local-dt #?(:cljs (time/to-default-time-zone dt)
                     :clj (time/to-time-zone (time.coerce/to-date-time dt)
                                             (if (= :default zone)
                                               (time/default-time-zone)
                                               (time/time-zone-for-id zone))))]
     [(time/year local-dt)
      (time/month local-dt)
      (time/day local-dt)])))
