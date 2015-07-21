;; ## 2d camera for game

;; So! What can it do?
;; * should know it's viewport size
;; * should know the bounds of the world within it can move
;; * should have a point of interest that it wants to keep in the center of view
;; * should be able to zoom? (how does that work? adjust viewport size? or a scale?
;;     * probably a scale

;; Maybe work in continuous time?
;;   try that later :)

;; It should produce a project matrix (eventually, when I decouple from gl11)

(ns game.camera
  (:require
    [cloj.math.vec2 :as v2 :refer [v2i v2f v2]]
    )
  )


(defrecord Camera [ a b c d ])


(defn mk-camera []
  (->Camera 1 2 3 4)
  )


(defn update-camera [cam]

  cam
  
  )
