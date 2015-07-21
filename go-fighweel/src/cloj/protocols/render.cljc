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

(defprotocol IRenderManager
  (init! [this])
  (make-spr! [this id img dims])
  (make-screen-renderer! [this])
  (make-render-target! [this dims]))

(defprotocol IRenderTarget
  (get-renderer [_])
  (activate! [_]))

(defprotocol IRenderBackend
  (ortho! [this window-dims canvas-dims])
  (save! [this])
  (restore! [this])
  (clear! [this col])

  (clear-all! [this col])

  (box! [this pos dims col])
  (spr! [this img pos])
  (spr-scaled! [this img pos dims]))

(defprotocol ITextureManager
  (add-texture! [this info])
  (create-new-texture! [this info]))

(defmacro render-to [scr & forms]
  `(let [r# (activate! ~scr)]
     (doto r#
       ~@forms
       )
     )
  )
