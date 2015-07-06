(ns ^:figwheel-always go-fighweel.core
    (:require
      [cloj.utils :as utils]
      [om.core :as om :include-macros true]
      [sablono.core :as html :refer-macros [html]]
      ))

(enable-console-print!)

(defonce app-state (atom {:text "Hello world!"}))

(om/root
  (fn [data owner]
    (reify om/IRender
      (render [_]
        (html
          [:div
           [:h1 (:text data)]
           [:p "A paragraph!"]
           [:p "Another paragraph!"]
           ]
          )
        )))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

