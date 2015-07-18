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

  [resource-manager renderer sprs]

  (let [make-spr! (partial rend-p/make-spr! renderer)
        load-img! (partial res-p/load-img! resource-manager)
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
                       [sprite-id (make-spr! sprite-id img dims)])
                     (map (kork sprs)))))
        (into {})))))

(defn mk-spr-printer [rend sprs]
  (reify
    IRenderBackend
    (spr! [this img-key pos]
      (let [img (img-key sprs)]
        (rend-p/spr! rend img pos)))))

;;; }}}


