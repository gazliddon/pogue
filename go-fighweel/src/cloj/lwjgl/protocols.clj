(ns cloj.lwjgl.protocols)

(defprotocol IOGLTexture
  (get-uv-coords [_])
  (bind-texture! [_]))
