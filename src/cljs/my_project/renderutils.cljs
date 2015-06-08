(ns gaz.renderutils
  (:require [gaz.renderprotocols :refer [matrix!
                                         identity!
                                         translate!
                                         scale!
                                         rotate!]]))

  
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

(defmethod xform! :default [^ITransformable r op _]
  (println (str "not implemented xform! op " op)))

(defn do-xforms! [r xforms]
  (doseq [xfd xforms]
    (apply xform! r xfd)))
