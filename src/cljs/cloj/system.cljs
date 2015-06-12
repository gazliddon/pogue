(ns cloj.system
  )

(defprotocol ISystem
  (get-resource-manager [_])
  (get-render-engine [_]))
