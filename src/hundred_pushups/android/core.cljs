(ns hundred-pushups.android.core
  (:require [reagent.core :as r :refer [atom]]
            [clojure.pprint :as pp]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [hundred-pushups.events]
            [hundred-pushups.subs]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def linking (.-Linking ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))

(def pushup-form-url "http://www.100pushups.com/perfect-pushups-posture/")

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn get-started []
  [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                        :on-press #(do
                                     (dispatch [:complete-stage :get-started])
                                     (dispatch [:db/save]))}
   [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Let's get started"]])

(defn learn-pushup-form []
  [view {}
   [text {:style {:text-align "center"}} "Before you start doing pushups, learn optimal pushup technique"]
   [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                        :on-press #(.openURL linking pushup-form-url)}
    [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Read article about form"]
    ]
   [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin 10}
                         :on-press #(do
                                      (dispatch [:complete-stage :learn-pushup-form])
                                      (dispatch [:db/save]))}
    [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "OK, I've got it."]]])

;; http://www.100pushups.com/max-pushups-test/
(defn do-pushup-test []
  (let [ui-state (subscribe [:ui-state/get])]
    (fn []
      [view {}
       [text {:style {:text-align "center"}} "Do as many good pushups as you can."]
       [text {:style {:text-align "center"}} "Stop when it takes more than five seconds to do a rep or when you can't do any more reps."]
       [text {} "Reps:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                    :default-value (:pushup-reps-text @ui-state)
                    :on-change-text (fn [text]
                                 (dispatch [:ui-state/set [:pushup-reps-text] text])
                                      (dispatch [:db/save]))}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin 10}
                             :on-press #(do
                                          (dispatch [:complete-stage :do-pushup-test])
                                          (dispatch [:db/save]))}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Done!"]]])))


(defn do-plank-test []
  (let [ui-state (subscribe [:ui-state/get])]
    (fn []
      [view {}
       [text {:style {:text-align "center"}} "Hold a plank for as many breaths as you can (not seconds!)"]
       [text {:style {:text-align "center"}} "Stop when your form starts to get bad (e.g. sagging body)"]
       [text {} "Plank breadths:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                    :default-value (:plank-reps-text @ui-state)
                    :on-change-text (fn [text]
                                 (dispatch [:ui-state/set [:plank-reps-text] text])
                                      (dispatch [:db/save]))}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin 10}
                             :on-press #(do
                                          (dispatch [:append-test
                                                     {:exr/pushup-reps (js/parseInt (:pushup-reps-text @ui-state))
                                                      :exr/plank-reps (js/parseInt (:plank-reps-text @ui-state))
                                                      }
                                                     ])
                                          (dispatch [:ui-state/clear [[:plank-reps-text]
                                                                      [:pushup-reps-text]]])
                                          (dispatch [:complete-stage :do-plank-test])
                                          (dispatch [:db/save]))}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Done!"]]])))

(defn invalid-stage []
  [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
   [text {:style {:font-size 20 :font-weight "100" :margin-bottom 10 :text-align "center"}} "Invalid stage!"]
   [text {} "The current stage is:"]
   [text {:style {:font-family "Menlo"}} (pr-str @(subscribe [:stage]))]
   [text {} "DB is:"]
   [text {:style {:font-family "Menlo"}} (pp/write @(subscribe [:db]) :stream nil)]])

(defn show-day []
  [view {:style {:flex-direction "column" :align-items "center"}}
   [text {:style {:font-size 20 :font-weight "100" :margin-bottom 10 :text-align "center"}} "Today's exercise"]
   (let [schedule @(subscribe [:days-exercise])
         circuit (:exr/circuit schedule)]
     [view {}
      (for [x (range (:exr/sets schedule))]
        [view {:key x}
         [text {:style {:font-size 18 :font-weight "600" :margin-top 10}} (str "Set" x)]
         [text {} (str (:exr/pushup-reps circuit) " pushups")]
         [text {} (str "Hold plank for "(:exr/plank-reps circuit) " breaths")]])
      [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                            :on-press #(do
                                         (dispatch [:complete-day schedule])
                                         (dispatch [:db/save]))}
       [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "I did it"]]])])

(defn set-schedule []
  (let [ui-state (subscribe [:ui-state/get])
        white-list (subscribe [:schedule/get-whitelist])]
    (fn []
      [view {}
       [text {:style {:font-size 20 :font-weight "100" :margin-bottom 10 :text-align "center"}} "Create Schedule"]
       ;;TODO how to do a select box
       [text {} "Day:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                        :default-value (:schedule-day-text @ui-state)
                        :on-change-text (fn [text]
                                     (dispatch [:ui-state/set [:schedule-day-text] text])
                                          (dispatch [:db/save]))}]
       [text {} "Start:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                        :default-value (:start-text @ui-state)
                        :on-change-text (fn [text]
                                     (dispatch [:ui-state/set [:start-text] text])
                                          (dispatch [:db/save]))}]
       [text {} "End:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                        :default-value (:end-text @ui-state)
                        :on-change-text (fn [text]
                                     (dispatch [:ui-state/set [:end-text] text])
                                          (dispatch [:db/save]))}]

       ;; TODO scrolling
       ;; TODO input validation
       ;; Refactor formatting to core out of UI
       ;; setup https://github.com/clj-time/clj-time
       ;; use JS plugins https://github.com/xgfe/react-native-datepicker https://www.npmjs.com/package/react-native-date-time-picker
       ;;[text {} "list:" (str @white-list)]
       ;;TODO ben why can't I use doseq
       ;;(doseq [[day times] @white-list]
       ;;  (fn []
       ;;   [view {:key day}
       ;;    [text {} "day:" (str day)]]))

       (for [row @white-list]
        [view {:key row}
         [text {:style {:font-size 18 :font-weight "600" :margin-top 10}} (str (name(first row)) ": " (clojure.string/join "-" (last row)))]])

       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                        :on-press #(do
                                     (dispatch [:save-white-list (:schedule-day-text @ui-state) (:start-text @ui-state) (:end-text @ui-state)])
                                     (dispatch [:db/save]))}
         [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Save Schedule"]]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                        :on-press #(do
                                     (dispatch [:ui-mode-set [:current-mode] :stages])
                                     (dispatch [:db/save]))}
         [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Back"]]
       ])))


(defn invalid-mode []
  [view {:style {:flex-direction "column" :align-items "center"}}
   [text {:style {:font-size 20 :font-weight "100" :margin-bottom 10 :text-align "center"}} "Bad user, how did you get here."]])

(defn show-stage [stage]
  [view {:style {:flex-direction "column" :align-items "center"}}
    [text {:style {:font-size 20 :font-weight "100" :margin-bottom 20 :text-align "center"}} stage]
    (case stage
      :get-started [get-started]
      :learn-pushup-form [learn-pushup-form]
      :do-pushup-test [do-pushup-test]
      :do-plank-test [do-plank-test]
      :show-day [show-day]
      [invalid-stage])
    [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                        :on-press #(do
                                     (dispatch [:ui-mode-set [:current-mode] :schedules])
                                     (dispatch [:db/save]))}
     [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Set Schedule"]]
    [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                             :on-press #(do
                                          (dispatch [:db/reset])
                                          (dispatch [:db/save]))}
     [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Reset"]]])

(defn app-root []
  (let [stage (subscribe [:stage])
        mode (subscribe [:ui-mode/get])]
    (fn []
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {:style {:font-size 40 :font-weight "100" :margin-bottom 10 :text-align "center"}} "100 Pushup Challenge"]
       [text {:style {:font-size 20 :font-weight "100" :margin-bottom 20 :text-align "center"}} "Become a pushup master"]

       (case @mode
         :stages [show-stage @stage]
         :schedules [set-schedule]
         [invalid-mode])
       ])))



(defn init []
  (dispatch-sync [:boot/init])
  (.registerComponent app-registry "HundredPushups" #(r/reactify-component app-root)))
