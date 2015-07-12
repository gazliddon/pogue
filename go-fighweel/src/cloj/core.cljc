(ns cloj.core
  (:require 
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p])

    (:import (org.lwjgl.input Keyboard))
  )



(defn main [sys]
  (let [window (sys-p/get-window sys) ]
    (do
      (win-p/create window 320 256 "rogue bow")

      (loop []
        (do
          (when-not (Keyboard/isKeyDown Keyboard/KEY_UP)
            (win-p/updater window)
            (when-not @quit?
              (recur))  
            )
          ))

      (win-p/destroy window ))))

