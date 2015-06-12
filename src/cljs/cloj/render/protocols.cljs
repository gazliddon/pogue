(ns cloj.render.protocols)

(defprotocol ITransformable
  (matrix! [this v])
  (identity! [this])
  (translate! [this v])
  (scale! [this v])
  (rotate! [this v]))

(defprotocol IRenderBackend
  (clear! [this info])
  (box! [this info])
  (spr! [this info])
  (spr-scaled! [this info]))

(defprotocol ITextureManager
  (add-texture! [this info])
  (create-new-texture! [this info]))


