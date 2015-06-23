;; Reqs {{{
(ns game.sprs
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ])
  (:require

    [game.sprdata           :refer [spr-data]]

    [cloj.resources.manager :as rman
                            :refer [create-render-target!
                                    load-img!
                                    clear-resources!  ]]

    [game.html              :refer [mk-system]]
    [cloj.resources.html    :as rmhtml]

    [cloj.math.misc         :refer [cos-01 log-base-n ceil floor num-digits]]
    [cloj.math.vec2         :as v2 :refer [v2]]
    [cloj.math.vec3         :as v3 :refer [vec3]]

    [cloj.system            :refer [get-resource-manager
                                    get-render-engine]]

    (cloj.render.protocols  :as rp)

    [cloj.web.utils         :refer [by-id log-js]]

    [cljs.core.async        :as async
                            :refer [put! >! chan <! alts! close! dropping-buffer mult tap]]


    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))
;; }}}

(defn mk-spr
  "Makes an IImage from  spr"
  ([img id [x y w h]]
   (reify rman/IImage
     (id [_] id)
     (dims [_]
       [x y w h])
     (width [_] w)
     (height [_] h)
     (img [_] (rman/img img))))
  
  ([img id x y w h]
   (mk-spr img id [x y w h])))

(defn dump [s v]
  (println s)
  (println v)
  (log-js v)
  (println ""))

(defn k->file-name [k] (str "data/" (name k) ".png"))

(defn img-map->load-chan  [rman img-map]
  (->> img-map
       (map (fn [k f] (rman/load-img! rman k f)))
       (async/merge)
       (async/into ())))

(defn get-load-info [spr-keys]
  (reduce (fn [m v]
            (assoc m v (k->file-name v))) {} spr-keys))

(defn load-sprs [rman sprs]
  (let [spr-chan (->> (map #((rman/load-img! rman % (k->file-name  %))) (keys sprs) )
                      (async/merge)
                      (async/into ()))
        ret-chan (chan) ]

    (go
      (put! ret-chan 
            (->>
              (<! spr-chan)
              (mapcat  (fn [i]
                         (let [img      (rman/img i)
                               spr-list ((rman/id i) sprs) ]
                           (map (fn [ [id & dims ] ] [id (mk-spr img id dims)] ) spr-list  ))))
              (into {}))))
    ret-chan))

(defn mk-spr-printer [spr-seq])


(do
  (def system (mk-system "game" "game-canvas"))

  (let [rman (get-resource-manager system)
        rend (get-render-engine system)
        spr-ch (load-sprs rman spr-data) ]

    (go
      (let [sprs (<! spr-ch)]
        (println "loaded")
        )
      )


    
    )
  
  
  )

;;; }}}


