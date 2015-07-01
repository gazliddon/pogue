(ns game.html
  (:require
    [cloj.html.utils              :as htmlu]
    [cloj.resources.html    :refer [mk-resource-manager]]
    [cloj.system            :refer [ISystem]]
    [cloj.render.canvas     :as canvas]
    [cloj.web.utils         :as utils :refer [log-js]]
    [dommy.core             :as dommy :include-macros true]    
    [cloj.web.utils         :refer [by-id]]
    [cloj.resources.manager :as rman]))


(defn mk-system [main-div-id canvas-id]
  (let [main-div-el (by-id main-div-id)
        [w h] (htmlu/get-window-dims)
        rm   (mk-resource-manager)
        rend (rman/create-render-target! rm canvas-id w h )]
    (do
      (rman/clear-resources! rm)

      (doto main-div-el
        (dommy/clear! )
        (dommy/append! (canvas/get-element rend)))

      (reify
        ISystem
        (log [_ txt]
          (.log js/console txt))

        (get-resource-manager [_]
          rm)

        (get-render-engine [_]
          rend
          ))  

      )
    ))


