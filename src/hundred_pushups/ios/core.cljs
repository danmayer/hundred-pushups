(ns hundred-pushups.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [hundred-pushups.events]
            [hundred-pushups.subs]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn get-started []
  )

(defn pushup-form []
  [:text {:value "test"}]
  )

(defn app-root []
  (let [last-stage (subscribe [:last-stage])
        _ (prn "last stage is" @last-stage)]
    (fn []
      [view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [text {:style {:font-size 40 :font-weight "100" :margin-bottom 10 :text-align "center"}} "100 Pushup Challenge"]
       [text {:style {:font-size 20 :font-weight "100" :margin-bottom 20 :text-align "center"}} "Become a pushup master"]
       (if @last-stage
         [text {:style {:color "red" :text-align "center" :font-weight "bold"}} "Test!"]
         [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                               :on-press #(do
                                            (dispatch [:append-progress :started])
                                            (dispatch [:db/save]))}
          [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Let's get started"]])
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5 :margin-top 20}
                             :on-press #(do
                                          (dispatch [:db/reset])
                                          (dispatch [:db/save]))}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "Reset"]]
       ])))

(defn init []
  (dispatch-sync [:boot/init])
  (.registerComponent app-registry "HundredPushups" #(r/reactify-component app-root)))
