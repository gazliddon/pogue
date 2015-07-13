(ns cloj.protocols.system)

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (get-window           [_])
  (get-loader           [_])
  (get-resource-manager [_])
  (get-render-engine    [_])
  (get-keyboard         [_])
  (get-msg-chan         [_]))

(defrecord ClojSystem
  [window
   loader
   resource-manager
   render-engine
   keyboard
   msg-chan ]

  ISystem
  (get-window           [_] window)
  (get-loader           [_] loader)
  (get-resource-manager [_] resource-manager)
  (get-render-engine    [_] render-engine)  
  (get-keyboard         [_] keyboard)  
  (get-msg-chan         [_] msg-chan))



