(ns cloj.render.utils
  (:require [cloj.render.protocols :as rp]))

; (defmulti xform! (fn [^ITransformable r op data] op))

; (defmethod xform! :matrix [^ITransformable r op data]
;   (rp/matrix! r data)) 

; (defmethod xform! :identity  [^ITransformable r op _]
;   (rp/identity! r))

; (defmethod xform! :translate  [^ITransformable r op data]
;   (rp/translate! r data))

; (defmethod xform! :scale [^ITransformable r op data] 
;   (rp/scale! r data))

; (defmethod xform! :rotate [^ITransformable r op data] 
;   (rp/rotate! r data))

; (defmethod xform! :clear [^IRenderBackend r op data]
;   (rp/clear! r data))

; (defmethod xform! :box [^IRenderBackend r op data]
;   (rp/box! r data))

; (defmethod xform! :spr [^IRenderBackend r op data]
;   (rp/spr! r data))

; (defmethod xform! :spr-scaled [^IRenderBackend r op data]
;   (rp/spr-scaled! r data))

; (defmethod xform! :default [^ITransformable r op data]
;   (println (str "not implemented xform! op " op data)))

; (defn do-xforms! [r xforms]
;   (doseq [xform xforms]
;     (xform! r (first xform) (rest xform))))

