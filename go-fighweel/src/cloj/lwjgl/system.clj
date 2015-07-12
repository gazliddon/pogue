(ns cloj.lwjgl.system
  (:require
    [cloj.jvm.resources       :refer [mk-resource-manager]]
    [cloj.protocols.system    :refer [ISystem ->ClojSystem]]
    [cloj.protocols.resources :as res-p]

    [cloj.lwjgl.window        :refer [mk-lwjgl-window]]
    [clojure.core.async :as async :refer [chan put! <! go-loop]]  
    [cloj.jvm.loader          :refer [mk-loader]])

  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl ContextAttribs Display DisplayMode GL11 GL15 GL20 GL30 PixelFormat)
           (org.lwjgl.util.glu GLU)))


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
                        (println "YEAH")
    
                        (reset! game-state :please-quit true))
                })

(defn mk-system []
  (let [loader   (mk-loader)
        msg-chan (mk-msg-center msg->func)
        sys      (->ClojSystem
                   (mk-lwjgl-window)
                   loader
                   (mk-resource-manager loader)
                   nil
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


