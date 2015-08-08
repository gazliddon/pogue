(ns cloj.lwjgl.system
  (:require
    [cloj.lwjgl.resources       :refer [mk-resource-manager]]

    [cloj.math.vec2 :as vec2 :refer [v2]]

    [cloj.protocols.system    :as sys-p ]
    [cloj.protocols.resources :as res-p]

    [cloj.lwjgl.keyboard :as keyb]
    [cloj.lwjgl.render :as render]
    [cloj.lwjgl.render2 :as render-2]

    [cloj.lwjgl.window            :refer [mk-lwjgl-window]]
    [clojure.core.async :as async :refer [chan put! <! go-loop]]  
    [cloj.jvm.loader              :refer [mk-loader]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-system []
  (let [sys (sys-p/mk-system
              (mk-lwjgl-window)
              (mk-resource-manager (mk-loader))
              (render-2/mk-lwjgl-render-manager )
              (keyb/mk-keyboard))]
    (do
      (res-p/clear-resources! (:resource-manager sys))
      sys
      )))


