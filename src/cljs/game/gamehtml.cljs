(ns game.html
  (:require
    [cloj.resources.html    :refer [mk-resource-manager]]
    [cloj.system            :refer [ISystem]]
    [cloj.resources.manager :refer [create-render-target!]]
    ))


(defn mk-system [resource-div-id canvas-id]
  (let [rm   ( mk-resource-manager )
        rend (create-render-target! rm canvas-id 100 100)]
    (reify
      ISystem

      (log [_ txt]
        (.log js/console txt))

      (get-resource-manager [_]
        rm)

      (get-render-engine [_]
        rend
        ))))

