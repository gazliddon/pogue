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

(def quit? (atom false))

(def tm (atom 0.0))

(defn funny-col [t]
  [(cos-01 t)
   (cos-01 (* t 3.1))
   (cos-01 (* t (cos-01 (* t 2))))
   1.0 ])

(def quit-keys [:key-escape :key-q])

(defn any-keys-pressed? [keypfn ks]
  (->
    (fn [r v]
      (or r (keypfn v)))
    (reduce false ks)))

(defn any-quit-keys-pressed? [keypfn] (any-keys-pressed? keypfn quit-keys))

(defn draw-frame [r t]
  (do
    (clear! r (funny-col t))
    (box! r (v2 10 10) (v2 100 100) [0 0 0 1])))

(defn main [sys]
  (let [window (sys-p/get-window sys)
        keyb   (sys-p/get-keyboard sys)
        key-pressed? #(:state (key-p/get-key-state keyb %))
        r (sys-p/get-render-engine sys)]

    (do
      (win-p/create! window (v2 640 480) "rogue bow")

      (try
        (loop [t 0]
          (do
            (key-p/update! keyb)
            (win-p/update! window)
            (draw-frame r t)
            (when-not (any-quit-keys-pressed? key-pressed?) 
              (recur (+ t (/ 1 60))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e))))

      (win-p/destroy! window ))))

