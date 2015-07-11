(ns cloj.protocols.window
  )

(defprotocol IWindow
  (create [_ w h title])
  (destroy [_])
  (updater [_]))
