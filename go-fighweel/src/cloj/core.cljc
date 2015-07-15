(ns cloj.core
  (:require 

    [game.gamekeys :as gamekeys :refer [mk-game-keys ]]

    [cloj.math.misc :refer [cos-01]]
    [cloj.math.vec2 :refer [v2]]
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.render    :as rend-p :refer [clear!
                                                 box!]]
    [cloj.protocols.keyboard  :as key-p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn funny-col [t]
  [(cos-01 t)
   (cos-01 (* t 3.1))
   (cos-01 (* t (cos-01 (* t 2))))
   1.0 ])

(defn draw-frame [r t]
  (do
    (clear! r (funny-col t))
    (box! r (v2 0 0) (v2 0.000001 0.1) (funny-col (+ 3 t)))))


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
        r (sys-p/get-render-engine sys) ]
    (do
      (win-p/create! window (v2 640 480) "rogue bow")

      (try
        (loop [t 0]
          (do
            (gamekeys/update! gkeys)

            (win-p/update! window)

            (draw-frame r t)

            (when-not (gamekeys/quit? gkeys) 
              (recur (+ t (/ 1 60))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e)))

        (finally
          (win-p/destroy! window ))))))

