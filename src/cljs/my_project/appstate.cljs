(ns gaz.appstate
  (:require [gaz.gamerender :as gr]
            )
  )

(def app-state
  (atom
    {:update 0
     :main-app { :name "Pogue" }
     :count {:count 0}
     :render-data gr/game-render-data 
     }))
