(ns game.player
  (:require
    [cloj.math.vec2         :as v2 :refer [vec2 vec2-s]]
    [cloj.utils             :refer [format map-difference] ]
    ))

(defprotocol IPlayer
  (get-pos [this t])
  )

(defrecord Player [intention]
  IPlayer
  (get-pos [ _ t]
    )
  )

(defn mk-player [t p]
  (->Player (->EaserV2 t p v2/zero v2/zero)))


; (def old-map {:proc-1 6502
;               :proc-2 68040
;               :proc-3 32016})

(def old-map {:proc-1 6510
              :proc-2 68040
              :proc-3 32016})

(def new-map {:proc-1 6502
              :proc-2 68040
              :proc-3 32016})


(println 
  (map-difference old-map new-map )

  )
