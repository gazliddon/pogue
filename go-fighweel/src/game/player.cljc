(ns game.player
  (                         :require
    [cloj.math.vec2         :as v2 :refer [vec2 vec2-s]]
    [cloj.utils             :refer [format map-difference] ]
    [cloj.math.misc         :refer [sin cos fract]]

    (cloj.render.protocols  :as rp)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IOscillator
  (get-p [this t]))

(defn square-wave [t]
  (if (> 0.5 (fract t))
    -1
    1))

;; Assuming func returns -1 1
(defrecord Oscillator [start-t phase amplitude freq func]
  IOscillator
  (get-p [_ t]
    (let [dt (- t start-t)
          t' (+ phase  (/ dt freq))
          v  (func t') ]
      (* amplitude v))))

(defmulti mk-osc (fn [kind _ _ _ _] kind))

(defmethod mk-osc :sin [_ start-t phase amplitude freq ]
  (->Oscillator start-t phase amplitude freq sin))

(defmethod mk-osc :cos [_ start-t phase amplitude freq ]
  (->Oscillator start-t phase amplitude freq cos))

(defmethod mk-osc :default [_ start-t phase amplitude freq ]
  (throw "not implemented mk-osc" ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IMovement
  (get-p-at-t [this t])
  (get-v-at-t [this t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v-osc [osc-x osc-y]
  (reify IMovement
    (get-p-at-t [this t]
      (vec2
        (get-p-at-t osc-x t)
        (get-p-at-t osc-y t)))

    (get-v-at-t [this t]
      (throw "not implemented get-v-at-t")
      )))

(defn mk-player [start-t]
  (let [pos (vec2 100 100)
        x-osc (mk-osc :cos start-t :cos (:x pos) 30 3 )
        y-osc (mk-osc :sin start-t (:y pos) 30 1 ) ]
    {:movement (v-osc x-osc y-osc)
     :frame :bub0}))

(defn update-player [player t]
  player)

(defn draw-player [spr-rend {:keys [movement frame] :as player} t]

  (comment rp/spr! spr-rend frame (get-p-at-t player t)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IPlayer
  (get-pos [this t])
  )

(defrecord Player [intention]
  IPlayer
  (get-pos [ _ t]
    )
  )

