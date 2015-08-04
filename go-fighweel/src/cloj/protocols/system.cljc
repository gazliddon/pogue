(ns cloj.protocols.system
  (:require
    [clojure.core.async :as async]))

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (get-window           [_])
  (get-resource-manager [_])
  (get-render-manager   [_])
  (get-keyboard         [_])
  (msg                  [_ topic payload])
  (sub                  [_ topic ch]))

(defrecord ClojSystem
  [window
   resource-manager
   render-manager
   keyboard
   pub
   ch]

  ISystem
  (get-window           [_] window)
  (get-resource-manager [_] resource-manager)
  (get-render-manager   [_] render-manager)  
  (get-keyboard         [_] keyboard)  

  (msg [_ topic payload]
    (async/put! ch {:topic topic :payload payload }))

  (sub [_ topic ch]
    (async/sub pub topic ch)))

(defn mk-system [window rezman render kb ]
  (let [ch (async/chan)
        pub (async/pub ch :topic )]
    (->ClojSystem window rezman render kb ch pub)))


