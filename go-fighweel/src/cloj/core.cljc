(ns cloj.core
  (:require 
    [cloj.protocols.system    :as sys-p]
    [cloj.protocols.window    :as win-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.keyboard  :as key-p]
    )
  )

(def quit? (atom false))

(defn main [sys]
  (let [window (sys-p/get-window sys)
        keyb   (sys-p/get-keyboard sys)
        key-pressed? #(:current (key-p/get-key-state keyb %)) ]

    (do
      (win-p/create window 320 256 "rogue bow")

      (loop []
        (do
          (key-p/update! keyb)
          (win-p/updater window)
          (when-not (key-pressed? key-p/K-0)
            (recur))))

      (win-p/destroy window ))))

