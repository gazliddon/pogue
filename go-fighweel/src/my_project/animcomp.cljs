(ns gaz.animcomp 
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [gaz.appstate :refer [app-state]]
    [om.core :as om :include-macros true]
    [cljs.core.async :refer [put! chan <! alts!]]
    [om.dom :as dom :include-macros true]))

(defn inc-time [v]
  (+ v (/ 1 60)))


(defn animation-view [app _]
  (reify
    om/IWillMount
    (will-mount [_]
      )
    om/IRender
    (render [_]
      (dom/div nil (:count app)))))
