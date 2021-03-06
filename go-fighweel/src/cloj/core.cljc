(ns cloj.core
  (:require 

    [game.sprs          :as sprs]
    [game.sprdata       :as sprdata]
    [clojure.core.async :as async    :refer [go <!! ]]
    [game.gamekeys      :as gamekeys :refer [mk-game-keys
                                             up?
                                             down?
                                             left?
                                             right?
                                             quit?
                                             ]]

    [cloj.utils :as utils :refer [<? <??]]

    [cloj.math.misc :refer [cos-01 cos sin]]
    [cloj.math.vec2 :as v2 :refer [v2 v2f]]
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.render    :as rend-p :refer [clear!
                                                 box!
                                                 ortho!
                                                 scale!
                                                 spr!
                                                 init!
                                                 clear-all!]]
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
                  v2/half))]
    (->>
      (v2/mul
        (v2f t t)
        scale)
      (v2/apply cos-01)
      (v2/mul (v2f 320 240)))))

(defn draw-snake [r t f]
  (do
    (let [cos-01-t (cos-01 t)
          cos-t (cos t)
          amount 300]
      (doseq [v (range amount)]
        (let [v-norm (/ v amount)
              v-scaled (* cos-t  (*  v-norm 8))
              v-t (+ t v cos-t)
              pos (f-pos (+ t v-scaled (cos t)))
              col [(cos-01 (+ (* t 20) (/ v 100))) (cos-01 (* v 0.10)) (cos-01 (+ t v)) 1]
              ]
          (f pos (v2 (+ 5  (cos-01 (* 2 v-t))) (+ 5 (cos-01 (* 3 (+ 1 v-t))))) col ))  
        ))
    ))



(defn draw-frame [dims r spr-printer t]

  (do
    (let [ ]
      (clear-all! r [0.1 0 0.1 0])
      (ortho! r dims (v2 320 240))
      (clear! r (funny-col (/ t 10)))
      (draw-snake r t
                  (fn [pos dims col]
                    (box! r pos dims col))
                  )
      (draw-snake r t
                  (fn [pos dims col]
                    (spr! spr-printer :bub0 pos))
                  )
      )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def key-defs
  {:quit  [:key-esc :key-q]
   :up    [:key-up :key-k] 
   :down  [:key-down :key-j] 
   :left  [:key-left :key-h] 
   :right [:key-right :key-l]
   :fire  [:key-space] })

(defn mk-game-sprs [res r]
  (let [sprs-chan (sprs/load-sprs res r sprdata/spr-data)
        sprs (<?? sprs-chan) ]
    (sprs/mk-spr-printer r sprs)))

(defn spr-maker [res-man r]
  (fn [file-name]
    (->>
      file-name
      (res-p/load-img! res-man) 
      (<??)
      (#(rend-p/make-spr! r :poo % (rend-p/dims %))))))

(defn get-frm [anim t]
  (let [anim-fn (anim sprdata/anim-data)]
    (anim-fn t)))

(defn draw-sprs [r spr-printer pos t]
  (let [frm (get-frm :bub-stand t) ]
    (rend-p/spr! spr-printer frm pos)))

;; Some stuff to control things on screen
(def func->vel
  [[right?  (v2  1  0)]
   [left?   (v2 -1  0)]
   [up?     (v2  0 -1)]
   [down?   (v2  0  1)] ])

(defn new-pos [keyb pos scale]
  (->>
    ;; filter only pressed keys
    (filter (fn [[func _]]
              (func keyb)) func->vel)
    ;; strip out the func and leave the vel
    (map (fn [[_ v]] (v2/mul scale v)))
    ;; reduce add them all together
    (reduce v2/add pos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn main [sys]
  (let [window    (sys-p/get-window sys)
        gkeys     (mk-game-keys (sys-p/get-keyboard sys) key-defs)
        r         (sys-p/get-render-engine sys)
        res-man   (sys-p/get-resource-manager sys)
        dims      (v2 640 480)
        make-spr! (spr-maker res-man r) ]

    (do
      (win-p/create! window dims "rogue bow")
      (init! r)
      (try
        (let [tex (make-spr! "test-data/blocks.png")
              spr-printer (mk-game-sprs res-man r)
              ]
          ; (println (rend-p/img tex) )
          (loop [t 0
                 pos (v2 3 3)]
            (do
              (win-p/update! window)
              (gamekeys/update! gkeys)

              (draw-frame dims r spr-printer t)
              (draw-sprs r spr-printer pos t)

              (when-not (quit? gkeys)
                (recur (+ t (/ 1 60))
                       (new-pos gkeys pos (v2 0.5 0.5)))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e)))

        (finally
          (win-p/destroy! window ))
        ))))

