(ns cloj.core
  (:require 

    [game.sprs          :as sprs]
    [game.sprdata       :as sprdata]
    [game.appstate      :as appstare :refer [add-time! get-time]]
    [clojure.core.async :as async    :refer [go <!! ]]

    [clojure.pprint :refer [pprint]]

    [game.levelrender   :as level-render]

    [game.tiledata      :as tile-data :refer [tile-data]]

    [game.gamekeys      :as gamekeys  :refer [mk-game-keys
                                              zoom-out?
                                              zoom-in?
                                              up?
                                              down?
                                              left?
                                              right?
                                              quit?]]

    [cloj.utils :as utils :refer [<? <??]]

    [cloj.math.misc           :refer [cos-01 cos sin clamp]]
    [cloj.math.vec2           :as v2 :refer [v2 v2f]]
    [cloj.math.protocols      :as m :refer [add sub div mul]]
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.render    :as rend-p :refer [clear!
                                                 box!
                                                 ortho!
                                                 scale!
                                                 identity!
                                                 translate!
                                                 spr!
                                                 init!
                                                 clear-all!
                                                 render-to
                                                 ]]
    [cloj.protocols.keyboard  :as key-p]))



;; =============================================================================
;; {{{ Shit Camera
(defn camera [current-pos desired-pos]
  (->
    (v2/sub desired-pos current-pos)
    (v2/div (v2 12 12))
    (v2/add current-pos)))

;; }}}

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
          (f v pos (v2 (+ 5  (cos-01 (* 2 v-t))) (+ 5 (cos-01 (* 3 (+ 1 v-t))))) col ))  
        ))
    ))



(defn draw-frame
  ([r win-dims canv-dims sprs t]
   (let [spr-printer (sprs/mk-spr-printer r sprs)]
     (do
       (clear-all! r [0.1 0 0.1 1])
       (ortho! r win-dims canv-dims)
       (clear! r (funny-col (/ t 10)))

       (draw-snake r (- 0 t) 1000
                   (fn [i pos dims col]
                     (box! r pos dims col)))

       (draw-snake r t 100
                   (fn [i pos dims col]
                     (spr! spr-printer (sprdata/get-spr i)  pos))))))

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
   :fire  [:key-space]

   :zoom-out [:key-minus :key-a]
   :zoom-in [:key-equals :key-s]
   })

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


(defn mk-level-spr! [render-manager spr-data ]
  (level-render/mk-level-spr! render-manager spr-data 30 30 tile-data)
  )

(defn handle-zoom
  ""
  [gkeys zoom-inc]
  (cond
    (zoom-out? gkeys) (- 0  zoom-inc)
    (zoom-in? gkeys) zoom-inc
    :default 0))

(defn interp-to [time-to-interp current desired]
  (add current  (div (sub desired current) time-to-interp)))

(defprotocol IEaser
  )


(defrecord Interpolator [from to])

(defn main [sys]
  (let [win-dims     (v2 840 480)
        canv-dims    (v2/mul (v2 1600 900) (v2 0.2 0.2))

        window          (create-window! sys win-dims)
        gkeys           (mk-game-keys (sys-p/get-keyboard sys) key-defs)
        render-manager  (sys-p/get-render-manager sys)
        screen          (rend-p/make-screen-renderer! render-manager)
        res-man         (sys-p/get-resource-manager sys)
        mid-scr         (v2/mul canv-dims v2/half)  ]

    (try
      (let [sprs (mk-game-sprs res-man render-manager)
            lev-spr (mk-level-spr! render-manager sprs) ]

        (loop [t 0
               pos (v2 3 3)
               cam-pos (v2 0 0)
               zoom 2]
          (do
            (let [desired-pos (->>
                                (v2 zoom zoom)
                                (v2/div mid-scr)
                                (v2/sub pos)
                                (v2/clamp (v2 0 0) (v2 1000 1000)))
                  new-zoom    (->>
                                (+ zoom (handle-zoom gkeys 0.1))
                                (clamp 0.1 10)
                                )
                  ]

              (win-p/update! window)
              (gamekeys/update! gkeys)

              (render-to screen
                (clear-all! (funny-col t))
                (ortho! win-dims canv-dims)
                (clear! [0 0 0 1])
                (scale! (v2 zoom zoom))
                (translate! (v2/sub v2/zero cam-pos))
                (rend-p/spr! lev-spr (v2 0 0))
                (draw-sprs sprs pos t))

              ;; FILTH! globals are eval
              (add-time! (/ 1 60))

              (when-not (quit? gkeys)
                (recur (+ t (/ 1 60))
                       (new-pos gkeys pos (v2 1.5 1.5))
                       (camera cam-pos desired-pos)
                       (interp-to 60 zoom new-zoom )
                       ))))))

      (catch Exception e
        (pprint e)
        (println "[Error in main] " (.getMessage e)))

      (finally
        (destroy-window! window)))))


