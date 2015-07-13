(ns cloj.core
  (:require 

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
    (box! r (v2 10 10) (v2 100 100) [0 0 0 1])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IGameKeys
  (update! [_])
  (quit?   [_])
  (up?     [_])
  (down?   [_])
  (left?   [_])
  (right?  [_])
  (fire?   [_]))

(defn any-keys-pressed? [keypfn ks]
  (->
    (fn [r v]
      (or r (keypfn v)))
    (reduce false ks)))

(defn mk-game-keys [keyb key-defs]
  (let [any-pressed? (fn [kk]
                       (any-keys-pressed?
                         (:state #(key-p/get-key-state keyb %))
                         (kk key-defs)))]

    (reify
      IGameKeys
      (update![_] (key-p/update! keyb))
      (quit?  [_] (any-pressed? :quit))
      (up?    [_] (any-pressed? :up))
      (down?  [_] (any-pressed? :down))
      (left?  [_] (any-pressed? :left))
      (right? [_] (any-pressed? :right))
      (fire?  [_] (any-pressed? :fire))
      ))
  )
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

        gkeys (->
                (sys-p/get-keyboard sys)
                (mk-game-keys key-defs))

        r (sys-p/get-render-engine sys)]

    (do
      (win-p/create! window (v2 640 480) "rogue bow")

      (try
        (loop [t 0]
          (do
            (update! gkeys)

            (win-p/update! window)

            (draw-frame r t)

            (when-not (quit? gkeys) 
              (recur (+ t (/ 1 60))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e))))

      (win-p/destroy! window ))))

