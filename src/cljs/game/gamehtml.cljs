(ns game.html
  (:require
    [cloj.resources.html    :refer [mk-resource-manager]]
    [cloj.system            :refer [ISystem]]
    [cloj.render.canvas     :as canvas]
    [cloj.web.utils         :as utils :refer [log-js]]
    [dommy.core             :as dommy :include-macros true]    
    [cloj.web.utils         :refer [by-id]]
    [cloj.resources.manager :as rman]))

(defn mk-system [resource-div-id canvas-id]
  (let [rm   ( mk-resource-manager resource-div-id)
        rend (rman/create-render-target! rm canvas-id 400 225)]
    (do
      (dommy/append! (by-id "app") (canvas/get-element rend )))

    (reify
      ISystem
      (log [_ txt]
        (.log js/console txt))

      (get-resource-manager [_]
        rm)

      (get-render-engine [_]
        rend
        ))))


