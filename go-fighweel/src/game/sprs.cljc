;; Reqs {{{
(ns game.sprs
  (:require 
    [clojure.reflect :refer [reflect]]
    [cloj.utils          :as utils :refer [<?]]
    [clojure.core.async  :as async :refer [go <!]])

  (:require
    [cloj.protocols.resources :as res-p ]
    [cloj.protocols.render :as rend-p  :refer [IRenderBackend
                                               IImage]]
    ))
;; }}}

(defn mk-spr
  "Makes an IImage from an img"
  ([img id [x y w h]]
   (reify IImage
     (id [_] id)
     (dims [_]
       [x y w h])
     (width [_] w)
     (height [_] h)
     (img [_] img)))
  
  ([img id x y w h]
   (mk-spr img id [x y w h])))

(defn- k->file-name [k] (str "resources/public/data/" (name k) ".png"))

(defn- get-img-file-names [sprs]
  (map k->file-name (keys sprs))) 


(defn ->>dump [txt v]
  (println txt " " v)
  v)


(defn load-sprs

  "Takes the spr defs in sprdata.cljc
   hoists out the image files
   and then starts them loading asynchronously
   and returns a chan"

  [resource-manager sprs]

  (let [load-img! (partial res-p/load-img! resource-manager)
        spr-chan (->> (get-img-file-names sprs) 
                      (map load-img!)
                      (async/merge)
                      (async/into ())) ]
    (go
      (->>
        (<? spr-chan)
        (map vector (keys sprs))
        (mapcat  (fn [ [kork img] ]
                   (->
                     (fn [[sprite-id dims]]
                       [sprite-id (mk-spr img sprite-id dims)])
                     (map (kork sprs)))))
        (into {})))))

(defn mk-spr-printer [rend sprs]
  (reify
    IRenderBackend
    (spr! [this img-key pos]
      (let [img (img-key sprs)]
        (rend-p/spr! rend img pos)))))

;;; }}}


