(ns cloj.protocols.system
  (:require
    [clojure.core.async :as async]))

(defprotocol ITime
  (get-time [_]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IMessageCenter
  (sub! [_ topic ch])
  (unsub! [_ ch])
  (msg! [_ topic payload]))

(defrecord MessageCenter [chan pub]
  IMessageCenter

  (sub! [_ topic ch]
    (async/sub pub topic ch))
  
  (unsub! [_ ch]
    ;; TODO assert here! or fix
    )

  (msg! [_ topic payload]
    (async/put! chan {:topic topic :payload payload })))


(defn mk-msg-center []
  (let [ch (async/chan)
        pub (async/pub ch :topic )]
    (->MessageCenter ch pub)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol ISystem
  (get-window           [_])
  (get-resource-manager [_])
  (get-render-manager   [_])
  (get-keyboard         [_])
  (get-msg-center       [_]))

(defrecord ClojSystem
  [window
   resource-manager
   render-manager
   keyboard
   msg ]

  ISystem
  (get-window           [_] window)
  (get-resource-manager [_] resource-manager)
  (get-render-manager   [_] render-manager)  
  (get-keyboard         [_] keyboard)  
  (get-msg-center       [_] msg))

(defn mk-system [window rezman render kb ]
  (->ClojSystem
    window rezman render kb (mk-msg-center)))


