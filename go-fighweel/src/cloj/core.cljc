(ns cloj.core
  (:require 
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.keyboard  :as key-p]
    )
  )

(defn main [sys]
  (let [window (sys-p/get-window sys)
        keyb   (sys-p/get-keyboard sys)]
    (do
      (win-p/create window 320 256 "rogue bow")

      (loop []
        (do
          (when-not (key-p/get-key-state keyb key-p/K-0)
            (win-p/updater window)
            (recur)
            )
          ))

      (win-p/destroy window ))))

