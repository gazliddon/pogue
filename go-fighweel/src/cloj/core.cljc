(ns cloj.core
  (:require 

    [game.gamekeys :as gamekeys :refer [mk-game-keys ]]

    [cloj.math.misc :refer [cos-01 cos sin]]
    [cloj.math.vec2 :as v2 :refer [v2 v2f]]
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.render    :as rend-p :refer [clear!
                                                 box!
                                                 ortho!
                                                 init! ]]
    [cloj.protocols.keyboard  :as key-p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn funny-col [t]
  [(cos-01 t)
   (cos-01 (* t 3.1))
   (cos-01 (* t (cos-01 (* t 2))))
   1.0 ])

(defn f-pos [t]

  (let [scale-base (v2f 0.93 0.822)
        scale-t (/ t 1000)
        scale (v2/add
                scale-base
                (v2/mul
                  scale-base
                  (v2f (cos scale-t) (sin (+ 1 scale-t )))
                  v2/half
                  )         )

        ]

    (->>
      (v2/mul
        (v2f t t)
        scale)
      (v2/apply cos-01)
      (v2/mul (v2f 16 9)))))

(defn draw-frame [dims r t]
  (do
    (let [cos-01-t (cos-01 t)
          cos-t (cos t)
          amount 100
          ]
      (ortho! r dims (v2 16 9))
      (clear! r (funny-col (/ t 10)))
      (doseq [v (range amount)]
        (let [v-norm (/ v amount)
              v-scaled (* cos-t  (*  v-norm 8))
              v-t (+ t v cos-t)
              pos (f-pos (+ t v-scaled (cos t)))
              col [(cos-01 (+ (* t 20) (/ v 100))) (cos-01 (* v 0.10)) (cos-01 (+ t v)) 1]
              ]
          (box! r pos (v2 (+ 0.5  (cos-01 (* 2 v-t))) (+ 0.5 (cos-01 (* 3 (+ 1 v-t))))) col ))  
        ))
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-defs
  {:quit  [:key-esc :key-q]
   :up    [:key-up :key-k] 
   :down  [:key-down :key-j] 
   :left  [:key-left :key-h] 
   :right [:key-right :key-l]
   :fire  [:key-space] })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn main [sys]
  (let [window (sys-p/get-window sys)
        gkeys (mk-game-keys (sys-p/get-keyboard sys) key-defs)
        r (sys-p/get-render-engine sys)
        dims (v2 640 480) ]
    (do
      (win-p/create! window dims "rogue bow")
      (init! r)
      (try
        (loop [t 0]
          (do
            (gamekeys/update! gkeys)
            (win-p/update! window)

            (draw-frame dims r t)

            (when-not (gamekeys/quit? gkeys) 
              (recur (+ t (/ 1 60))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e)))

        (finally
          (win-p/destroy! window ))))))

