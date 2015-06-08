(ns gaz.appstate
  (:require [gaz.gamerender :as gr]))

(def app-state
  (atom
    {:main-app { :name "Pogue" }
     :tick 0
     :level-render-data gr/level-render-data 
     :main-render-data  gr/main-render-data 
     }))
