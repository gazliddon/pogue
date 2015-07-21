;; Reqs {{{
(ns game.sprs
  (:require 
    [clojure.reflect :refer [reflect]]
    [cloj.utils          :as utils :refer [<?]]
    [clojure.core.async  :as async :refer [go <!]]

    [game.sprdata :refer [spr-data]]
    )

  (:require
    [cloj.protocols.resources :as res-p ]
    [cloj.protocols.render :as rend-p  :refer [IRenderBackend
                                               IImage]]
    ))
;; }}}

(defn ->>dump [txt v] (println txt " " v) v)
(def resource-base-dir "resources/public/data/")
(defn- k->file-name [k] (str resource-base-dir (name k) ".png"))

(defn load-image-with-id! [res id]
  (let [fname (k->file-name id)]
    (go
      (->>
        (res-p/load-img! res fname)
        (<!)
        (assoc {:id id} :img )))))

(defn load-sprs

  "Takes the spr defs in sprdata.cljc
   hoists out the image files
   and then starts them loading asynchronously
   and returns a chan"

  [resource-manager render-manager sprs]

  (let [make-spr! (partial rend-p/make-spr! render-manager)
        load-img! (partial res-p/load-img! resource-manager)
        spr-chan (->> (keys sprs) 
                      (map
                        #(load-image-with-id! resource-manager %))
                      (async/merge)
                      (async/into ())) ]
    (go
      (->>
        (<? spr-chan)
        (mapcat  (fn [ {kork :id img :img} ]
                   (->
                     (fn [[sprite-id dims]]
                       [sprite-id (make-spr! sprite-id img dims)])
                     (map (kork sprs)))))
        (into {})))))

(defn mk-spr-printer
  "Take a render target and a database
   of spr defintions
   Return a renderbackend that will
   print sprs to the renderer "
  
  [rend sprs]

  (reify
    IRenderBackend
    (spr! [this img-key pos]
      (let [img (img-key sprs)]
        (rend-p/spr! rend img pos)))))

;;; }}}


