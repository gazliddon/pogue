(ns cloj.lwjgl.protocols)

(defprotocol IGLFBO
  (bind-fbo! [_])
  (has-z? [_]))

(defprotocol IGLTexture
  (get-uv-coords [_])
  (bind-texture! [_]))

