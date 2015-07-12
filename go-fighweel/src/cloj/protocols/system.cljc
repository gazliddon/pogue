(ns cloj.protocols.system)

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (get-window           [_])
  (get-loader           [_])
  (get-resource-manager [_])
  (get-render-engine    [_])
  (get-msg-chan         [_]))

(defrecord ClojSystem
  [window
   loader
   resource-manager
   render-engine
   msg-chan ]

  ISystem
  (get-window           [_] window)
  (get-msg-chan         [_] msg-chan)
  (get-loader           [_] loader)
  (get-resource-manager [_] resource-manager)
  (get-render-engine    [_] render-engine) )



