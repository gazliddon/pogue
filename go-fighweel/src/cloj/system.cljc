(ns cloj.protocols.system)

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (log                  [_ txt])
  (get-keyboard         [_])
  (get-loader           [_])
  (get-timer            [_])
  (get-resource-manager [_])
  (get-render-engine    [_]))

(defprotocol ISystem!
  (init!                  [_])
  (update!                [_])
  (destroy!               [_]))

(defrecord ClojSystem
  [logger
   keyboard
   loader
   timer
   resource-mananager
   render-engine]

  ISystem
  (log                  [_ txt] (logger txt))
  (get-keyboard         [_] keyboard)
  (get-loader           [_] loader)
  (get-timer            [_] timer)
  (get-resource-manager [_] resource-mananager)
  (get-render-engine    [_] render-engine) 
  )



