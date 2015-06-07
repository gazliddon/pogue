(ns gaz.appstate
  (:require [gaz.gamerender :as gr]
            )
  )

(def app-state
  (atom
    {:update 0
     :text "Little Shit Game"

     :main-app { :name "Little Shit Game" }

     :count {:count 0}
     :render-data gr/game-render-data 

     }))
