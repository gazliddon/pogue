(ns cloj.protocols.system)

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (get-window           [_])
  (get-resource-manager [_])
  (get-render-manager   [_])
  (get-keyboard         [_])
  (get-msg-chan         [_]))

(defrecord ClojSystem
  [window
   resource-manager
   render-manager
   keyboard
   msg-chan ]

  ISystem
  (get-window           [_] window)
  (get-resource-manager [_] resource-manager)
  (get-render-manager   [_] render-manager)  
  (get-keyboard         [_] keyboard)  
  (get-msg-chan         [_] msg-chan))



