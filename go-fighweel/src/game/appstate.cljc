(ns game.appstate
  ; (:require [experiments.depdelay :refer [dep-delay]])
  )

(defonce session-atom (atom 0))

(def app-state (atom {:time 0 }))

(defn get-time []
  (-> @app-state :time))

(defn inc-session [ ]
  (swap! session-atom inc))

(defn add-time!
  "add this amount to the global time"
  [v]
  (swap! app-state assoc :time (+ v (get-time))))


