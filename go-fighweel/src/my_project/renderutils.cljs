(ns gaz.renderutils
  (:require [gaz.renderprotocols :refer [matrix!
                                         identity!
                                         translate!
                                         scale!
                                         rotate!

                                         clear! 
                                         box! 
                                         spr! 
                                         spr-scaled!  ]]))

(defmulti xform! (fn [^ITransformable r op data] op))

(defmethod xform! :matrix [^ITransformable r op data]
  (matrix! r data)) 

(defmethod xform! :identity  [^ITransformable r op _]
  (identity! r))

(defmethod xform! :translate  [^ITransformable r op data]
  (translate! r data))

(defmethod xform! :scale [^ITransformable r op data] 
  (scale! r data))

(defmethod xform! :rotate [^ITransformable r op data] 
  (rotate! r data))

(defmethod xform! :clear [^IRenderBackend r op data]
  (clear! r data))

(defmethod xform! :box [^IRenderBackend r op data]
  (box! r data))

(defmethod xform! :spr [^IRenderBackend r op data]
  (spr! r data))

(defmethod xform! :spr-scaled [^IRenderBackend r op data]
  (spr-scaled! r data))

(defmethod xform! :default [^ITransformable r op data]
  (println (str "not implemented xform! op " op data)))

(defn do-xforms! [r xforms]
  (doseq [xform xforms]
    (xform! r (first xform) (rest xform))))


