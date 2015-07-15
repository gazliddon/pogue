(ns cloj.protocols.render)

(defprotocol IImage
  (id [_])
  (dims [_])
  (width [_])
  (height [_])
  (img [_]))

(defprotocol ITransformable
  (matrix! [this v])
  (identity! [this])
  (mul! [this v])
  (translate! [this v])
  (scale! [this v])
  (rotate! [this v]))

(defprotocol IRenderViewport
  (set-window-size! [this dims]))

(defprotocol IRenderBackend
  (ortho! [this window-dims canvas-dims])
  (init! [this])
  (save! [this])
  (restore! [this])
  (clear! [this col])
  (box! [this pos dims col])
  (spr! [this img pos])
  (spr-scaled! [this img pos dims]))

(defprotocol ITextureManager
  (add-texture! [this info])
  (create-new-texture! [this info]))


