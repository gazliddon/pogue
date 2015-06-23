;; Reqs {{{
(ns game.sprs
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require
    [cloj.resources.manager :as rman ]
    (cloj.render.protocols  :as rp)
    [cljs.core.async        :as async]
    ))
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
     (img [_] img)))
  
  ([img id x y w h]
   (mk-spr img id [x y w h])))

(defn- k->file-name [k] (str "data/" (name k) ".png"))

(defn- img-map->load-chan  [rman img-map]
  (->> img-map
       (map (fn [k f] (rman/load-img! rman k f)))
       (async/merge)
       (async/into ())))

(defn- get-load-info [spr-keys]
  (reduce (fn [m v]
            (assoc m v (k->file-name v))) {} spr-keys))

(defn load-sprs [resource-manager sprs]
  (let [load-img! (partial rman/load-img! resource-manager)
        spr-chan (->> (map #(load-img! % (k->file-name  %)) (keys sprs) )
                      (async/merge)
                      (async/into ()))
        ret-chan (async/chan) ]

    (go
      (async/put! ret-chan 
            (->>
              (<! spr-chan)
              (mapcat  (fn [i]
                         (->
                           (fn [[sprite-id dims]]
                             [sprite-id (mk-spr (rman/img i) sprite-id dims)])
                           (map ((rman/id i) sprs)))))
              (into {}))))
    ret-chan))

(defn mk-spr-printer [rend sprs]
  (reify
    rp/IRenderBackend
    (spr! [this img-key pos]
      (let [i-img (img-key sprs)]
        (rp/spr! rend i-img pos)))))

;;; }}}


