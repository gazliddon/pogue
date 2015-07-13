(ns cloj.protocols.window
  )

(defprotocol IWindow
  (create! [_ dims title])
  (destroy! [_])
  (update! [_]))
