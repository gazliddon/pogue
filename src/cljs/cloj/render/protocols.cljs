(ns cloj.render.protocols)

(defprotocol IImage
  (id [_])
  (dims [_])
  (width [_])
  (height [_])
  (img [_]))

(defprotocol ITransformable
  (matrix! [this v])
  (identity! [this])
  (translate! [this v])
  (scale! [this v])
  (rotate! [this v]))

(defprotocol IRenderBackend
  (save! [this])
  (restore! [this])
  (clear! [this col])
  (box! [this pos dims col])
  (spr! [this img pos])
  (spr-scaled! [this img pos dims]))

(defprotocol ITextureManager
  (add-texture! [this info])
  (create-new-texture! [this info]))


