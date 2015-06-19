(ns cloj.system
  )

(defprotocol ISystem
  (log [_ txt])
  (get-resource-manager [_])
  (get-render-engine [_]))
