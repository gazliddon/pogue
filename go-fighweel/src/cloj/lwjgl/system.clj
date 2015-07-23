(ns cloj.lwjgl.system
  (:require
    [cloj.lwjgl.resources       :refer [mk-resource-manager]]

    [cloj.math.vec2 :as vec2 :refer [v2]]

    [cloj.protocols.system    :refer [ISystem ->ClojSystem]]
    [cloj.protocols.resources :as res-p]

    [cloj.lwjgl.keyboard :as keyb]
    [cloj.lwjgl.render :as render]
    [cloj.lwjgl.render2 :as render-2]

    [cloj.lwjgl.window            :refer [mk-lwjgl-window]]
    [clojure.core.async :as async :refer [chan put! <! go-loop]]  
    [cloj.jvm.loader              :refer [mk-loader]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn unknown-msg [_]
  (print "UNKNOWN MESSAGE"))

(defn mk-msg-center [msg-table]
  (let [ch (chan)]
    (go-loop []
      (let [[com & args] (<! ch)
            func (get msg-table com unknown-msg)]
        (apply func args)
        (recur)))
    ch))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def game-state (atom {:please-quit  false
                       }))

(def msg->func {:quit (fn [sys]
                        (reset! game-state :please-quit true)) })

(defn mk-system []
  (let [msg-chan (mk-msg-center msg->func)
        sys      (->ClojSystem
                   (mk-lwjgl-window)
                   (mk-resource-manager (mk-loader))
                   (render-2/mk-lwjgl-render-manager )
                   (keyb/mk-keyboard)
                   (chan))]
    (do
      (res-p/clear-resources! (:resource-manager sys))
      (go-loop []
        (let [v (<! (:msg-chan sys))]
          (cond
            (keyword? v) (put! msg-chan [v sys])
            (vector? v) (put! msg-chan (into [(first v) sys] (rest v))) 
            :else (throw (Exception. "whooops!"))))
        (recur))
      sys
      )))


