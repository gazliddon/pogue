(ns cloj.system
  )

(defprotocol ITime
  (get-time [_]))

(defprotocol ISystem
  (log                  [_ txt])
  (get-timer            [_])
  (get-resource-manager [_])
  (get-render-engine    [_]))
