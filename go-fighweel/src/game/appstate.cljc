(ns game.appstate
  )

(def app-state (atom {:time 0}))

(defn get-time []
  (-> @app-state :time))

(defn add-time!
  "add this amount to the global time"
  [v]
  (swap! app-state assoc :time (+ v (get-time))))


