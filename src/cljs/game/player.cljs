(ns game.player
  (                         :require
    [cloj.math.vec2         :as v2 :refer [vec2 vec2-s]]
    [cloj.utils             :refer [format map-difference] ]
    [cloj.math.misc         :refer [sin cos fract]]
    ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IOscillator
  (get-v [this t]))

(defn square-wave [t]
  (if (> 0.5 (fract t))
    -1
    1))

;; Assuming func returns -1 1
(defrecord Oscillator [start-t phase amplitude freq func]
  IOscillator
  (get-v [_ t]
    (let [dt (- t start-t)
          t' (+ phase  (/ dt freq))
          v  (func t') ]
      (* amplitude v))))

(defmulti mk-osc (fn [kind _ _ _ _] kind))

(defmethod mk-osc :sin [start-t phase amplitude freq ]
  (->Oscillator start-t phase amplitude freq sin))

(defmethod mk-osc :cos [start-t phase amplitude freq ]
  (->Oscillator start-t phase amplitude freq cos))

(defmethod mk-osc :default [start-t phase amplitude freq ]
  (throw "not implemented"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IMovement
  (get-p-at-t [this t])
  (get-v-at-t [this t]))

(defn mk-sin-movement [start-t pos y-scale x-scale]
  (reify
    IMovement

    (get-p-at-t [this t]
      )

    (get-v-at-t [this t]) 
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IPlayer
  (get-pos [this t])
  )

(defrecord Player [intention]
  IPlayer
  (get-pos [ _ t]
    )
  )

; (defn mk-player [t p]
;   (->Player (->EaserV2 t p v2/zero v2/zero)))


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
