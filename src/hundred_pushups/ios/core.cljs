(ns hundred-pushups.ios.core
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
(def scroll-view (r/adapt-react-class ReactNative.ScrollView))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def tab-bar (r/adapt-react-class ReactNative.TabBarIOS))
(def tab-bar-item (r/adapt-react-class ReactNative.TabBarIOS.Item))

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
  (let [ui-state (subscribe [:ui-state/get])
        error-message (atom nil)]
    (fn []
      [view {}
       [text {:style {:text-align "center"}} "Do as many good pushups as you can."]
       [text {:style {:text-align "center"}} "Stop when it takes more than five seconds to do a rep or when you can't do any more reps."]
       (when @error-message
         [text {:style {:background-color "pink"}}
          @error-message])
       [text {} "Reps:"]
       [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                    :default-value (:pushup-reps-text @ui-state)
                    :keyboard-type "numeric"
                    :on-change-text (fn [text]
                                      (if (re-find #"^\d*$" text)
                                        (do
                                          (reset! error-message nil)
                                          (dispatch [:ui-state/set [:pushup-reps-text] text])
                                          (dispatch [:db/save]))
                                        (reset! error-message "Must be a valid number")))}]
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
                    :keyboard-type "numeric"
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

(defn show-day [day]
  [view {:style {:flex-direction "column" :align-items "center"}}
   [text {:style {:font-size 20 :font-weight "100" :margin-bottom 10 :text-align "center"}} "Today's exercise"]
   (let [circuit (:exr/suggested-circuit day)
         ui-state @(subscribe [:ui-state/get])]
     [view {:style {:height 350 :width 250 :padding 20 :background-color "lightgrey"}}
      [scroll-view {:style {:flex 1}}
       (for [x (range (:exr/sets day))]
         [view {:key x}
          [text {:style {:font-size 18 :font-weight "600" :margin-top 10}} (str "Set" x)]
          [text {} (str (:exr/pushup-reps circuit) " pushups")]
          [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                       :default-value (str (:exr/pushup-reps circuit))
                       :keyboard-type "numeric"
                       :on-change-text (fn [text]
                                         (dispatch [:ui-state/set [:pushup-reps-text x] text])
                                         (dispatch [:db/save]))}]
          [text {} (str "Hold plank for "(:exr/plank-reps circuit) " breaths")]
          [text-input {:style {:height 40 :border-color "grey" :border-width 1}
                       :default-value (str (:exr/plank-reps circuit))
                       :keyboard-type "numeric"
                       :on-change-text (fn [text]
                                         (dispatch [:ui-state/set [:plank-reps-text x] text])
                                         (dispatch [:db/save]))}]])]
      [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                            :on-press #(do
                                         (dispatch [:complete-day day ui-state])
                                         (dispatch [:db/save]))}
       [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Done!"]]]
     )])

(def base64-icon "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEsAAABLCAQAAACSR7JhAAADtUlEQVR4Ac3YA2Bj6QLH0XPT1Fzbtm29tW3btm3bfLZtv7e2ObZnms7d8Uw098tuetPzrxv8wiISrtVudrG2JXQZ4VOv+qUfmqCGGl1mqLhoA52oZlb0mrjsnhKpgeUNEs91Z0pd1kvihA3ULGVHiQO2narKSHKkEMulm9VgUyE60s1aWoMQUbpZOWE+kaqs4eLEjdIlZTcFZB0ndc1+lhB1lZrIuk5P2aib1NBpZaL+JaOGIt0ls47SKzLC7CqrlGF6RZ09HGoNy1lYl2aRSWL5GuzqWU1KafRdoRp0iOQEiDzgZPnG6DbldcomadViflnl/cL93tOoVbsOLVM2jylvdWjXolWX1hmfZbGR/wjypDjFLSZIRov09BgYmtUqPQPlQrPapecLgTIy0jMgPKtTeob2zWtrGH3xvjUkPCtNg/tm1rjwrMa+mdUkPd3hWbH0jArPGiU9ufCsNNWFZ40wpwn+62/66R2RUtoso1OB34tnLOcy7YB1fUdc9e0q3yru8PGM773vXsuZ5YIZX+5xmHwHGVvlrGPN6ZSiP1smOsMMde40wKv2VmwPPVXNut4sVpUreZiLBHi0qln/VQeI/LTMYXpsJtFiclUN+5HVZazim+Ky+7sAvxWnvjXrJFneVtLWLyPJu9K3cXLWeOlbMTlrIelbMDlrLenrjEQOtIF+fuI9xRp9ZBFp6+b6WT8RrxEpdK64BuvHgDk+vUy+b5hYk6zfyfs051gRoNO1usU12WWRWL73/MMEy9pMi9qIrR4ZpV16Rrvduxazmy1FSvuFXRkqTnE7m2kdb5U8xGjLw/spRr1uTov4uOgQE+0N/DvFrG/Jt7i/FzwxbA9kDanhf2w+t4V97G8lrT7wc08aA2QNUkuTfW/KimT01wdlfK4yEw030VfT0RtZbzjeMprNq8m8tnSTASrTLti64oBNdpmMQm0eEwvfPwRbUBywG5TzjPCsdwk3IeAXjQblLCoXnDVeoAz6SfJNk5TTzytCNZk/POtTSV40NwOFWzw86wNJRpubpXsn60NJFlHeqlYRbslqZm2jnEZ3qcSKgm0kTli3zZVS7y/iivZTweYXJ26Y+RTbV1zh3hYkgyFGSTKPfRVbRqWWVReaxYeSLarYv1Qqsmh1s95S7G+eEWK0f3jYKTbV6bOwepjfhtafsvUsqrQvrGC8YhmnO9cSCk3yuY984F1vesdHYhWJ5FvASlacshUsajFt2mUM9pqzvKGcyNJW0arTKN1GGGzQlH0tXwLDgQTurS8eIQAAAABJRU5ErkJggg==")

(defn app-root []
  (let [stage (subscribe [:stage])]
    (fn []
      [tab-bar {}
       [tab-bar-item {:title "work out"
                      :selected true
                      :icon {:uri base64-icon :scale 3}}
        [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
         [text {:style {:font-size 40 :font-weight "100" :margin-bottom 10 :text-align "center"}} "100 Pushup Challenge"]
         [text {:style {:font-size 20 :font-weight "100" :margin-bottom 20 :text-align "center"}} "Become a pushup master"]
         (case @stage
           :get-started [get-started]
           :learn-pushup-form [learn-pushup-form]
           :do-pushup-test [do-pushup-test]
           :do-plank-test [do-plank-test]
           :show-day (let [day @(subscribe [:days-exercise])]
                       (if (= day :exr/do-test)
                         [do-pushup-test]
                         [show-day day]))
           [invalid-stage])
         [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                               :on-press #(do
                                            (dispatch [:db/reset])
                                            (dispatch [:db/save]))}
          [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Reset"]]
         ]
        ]
       [tab-bar-item {:title "schedule"
                      :selected false
                      :icon {:uri base64-icon :scale 3}}
        [text {} "schedule stuff here"]
        ]
       [tab-bar-item {:title "dev tools"
                      :selected false
                      :icon {:uri base64-icon :scale 3}}
        [text {} "devtools"]]])))

(defn init []
  (dispatch-sync [:boot/init])
  (.registerComponent app-registry "HundredPushups" #(r/reactify-component app-root)))
