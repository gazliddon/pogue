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
                                                 activate!
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

(defn snake-pos [t amount i]
  (let [cos-01-t (cos-01 t)
        cos-t (cos t)
        v-norm (/ i amount)
        v-scaled (* cos-t  (*  v-norm 8))
        v-t (+ t i cos-t)
        pos (f-pos (+ t v-scaled cos-t)) ]
    pos))

(defn draw-snake [r t amount f]
  (do
    (let [cos-01-t (cos-01 t)
          cos-t (cos t) ]
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

(defn draw-frame
  ([r win-dims canv-dims sprs t]
   (let [spr-printer (sprs/mk-spr-printer r sprs)]
     (do
       (clear-all! r [0.1 0 0.1 0])
       (ortho! r win-dims canv-dims)
       (clear! r (funny-col (/ t 10)))

       (draw-snake r t 300
                   (fn [pos dims col]
                     (box! r pos dims col)))

       (draw-snake r t 30
                   (fn [pos dims col]
                     (spr! spr-printer :green-pepper  pos))))))

  ([r dims sprs t]
   (draw-frame r dims dims sprs t)))

(defn mk-sprs [amount]
  (->
    (fn [i]
      {:frame (sprdata/rand-spr)
       :pos (v2 (rand-int 320) (rand-int 240)) })
    (map (range amount))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def key-defs
  {:quit  [:key-esc :key-q]
   :up    [:key-up :key-k] 
   :down  [:key-down :key-j] 
   :left  [:key-left :key-h] 
   :right [:key-right :key-l]
   :fire  [:key-space] })

(defn mk-game-sprs [res render-manager]
  (let [sprs-chan (sprs/load-sprs res render-manager sprdata/spr-data)
        sprs (<?? sprs-chan) ]
    sprs))

(defn get-frm [anim t]
  (let [anim-fn (anim sprdata/anim-data)]
    (anim-fn t)))

(defn draw-sprs [r sprs pos t]
  (let [frm (get-frm :bub-stand t)
        printer (sprs/mk-spr-printer r sprs)]
    (rend-p/spr! printer frm pos)))

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
(defn create-window! [sys win-dims]
  (let [window (sys-p/get-window sys)
        rmanager (sys-p/get-render-manager sys)]
    (do
      (win-p/create! window win-dims "rogue bow")
      (init! rmanager)
      window
      )))

(defn destroy-window! [window]
  (win-p/destroy! window))

(defn main [sys]
  (let [win-dims  (v2 640 480)
        canv-dims (v2 320 240)
        off-scr-dims (v2 512 512)

        window          (create-window! sys win-dims)
        gkeys           (mk-game-keys (sys-p/get-keyboard sys) key-defs)
        render-manager  (sys-p/get-render-manager sys)
        screen          (rend-p/make-screen-renderer! render-manager)
        off-screen      (rend-p/make-render-target! render-manager off-scr-dims)
        res-man         (sys-p/get-resource-manager sys) ]

    (do
      (try
        (let [sprs (mk-game-sprs res-man render-manager) ]
          (loop [t 0
                 pos (v2 3 3)]
            (do
              (win-p/update! window)
              (gamekeys/update! gkeys)

              (let [r (activate! off-screen)]
                (doto r
                  (draw-frame off-scr-dims sprs t)
                  ))

              (let [r (activate! screen)]
                (doto r
                  (draw-sprs sprs pos t) 
                  (draw-frame win-dims canv-dims sprs t)
                  (rend-p/spr! off-screen pos)
                  ))

              (when-not (quit? gkeys)
                (recur (+ t (/ 1 60))
                       (new-pos gkeys pos (v2 0.5 0.5)))))))

        (catch Exception e
          (println "[Error in main] " (.getMessage e)))

        (finally
          (destroy-window! window)
          )
        ))))

