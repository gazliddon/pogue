(ns game.html
  (:require
    [cloj.resources.html    :refer [mk-resource-manager]]
    [cloj.system            :refer [ISystem]]
    ; [cloj.math.vec2         :refer [v2]]
    ; [cloj.resources.manager :refer [create-render-target!]]
    ; [cloj.render.canvas     :as canvas-render]
    [cloj.web.utils         :refer [by-id]]
    [cloj.resources.manager :as rman]
    ))

(defn mk-system [resource-div-id canvas-id]
  (print "here we go!")
  (let [rm   ( mk-resource-manager resource-div-id)
        rm2 (mk-resource-manager "app")
        rend (rman/create-render-target! rm2 canvas-id 400 225)]
    (reify
      ISystem
      (log [_ txt]
        (.log js/console txt))

      (get-resource-manager [_]
        rm)

      (get-render-engine [_]
        rend
        ))))


