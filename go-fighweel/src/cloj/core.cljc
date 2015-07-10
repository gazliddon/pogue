(ns cloj.core
  (:require [speclj.core :refer :all]
            [cloj.core :refer :all]
            [digest :as digest]
            [cloj.keyboard :as kb]
            [cloj.lwjgl.resources :as ac]
            [cloj.lwjgl.renderer :as rend]
            [cloj.resources.manager :as rman]
            [cloj.lwjgl.system :as sys]
            [clojure-gl.texture :as cgltex]
            [clojure.core.async :as async :refer [timeout
                                                  chan >! <!! <!
                                                  go
                                                  alts!!
                                                  pub sub
                                                  go-loop
                                                  put!
                                                  ]]
            [cloj.math.misc :refer :all]))


(do
(defn diff-2 [^long n]
  (*
   (/ 1 12)
   (- n 1)
   n
   (+ n 1)
   (+ (* 3 n) 2)))  )


(comment
  ;; Mess around with pub / sub
  ;; seems good

  (do
    (def in-chan (chan 10000))

    (def pubber (pub in-chan (fn [[t v]]
                               (println "topic" t)
                               t)))

    (def shoes-chan (chan))
    (sub pubber :shoes shoes-chan)

    (put! in-chan [:shoes "val"] )

    (println (<!! shoes-chan))
    ))




