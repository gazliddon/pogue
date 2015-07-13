(ns cloj.lwjgl.keyboard
  (:require 
    [cloj.protocols.keyboard :as keyb-p])
    (:import (org.lwjgl.input Keyboard)))

(def cloj->lwjgl
  {
   keyb-p/K-UP    Keyboard/KEY_UP 
   keyb-p/K-DOWN  Keyboard/KEY_DOWN
   keyb-p/K-LEFT  Keyboard/KEY_LEFT
   keyb-p/K-RIGHT Keyboard/KEY_RIGHT
   keyb-p/K-SPACE Keyboard/KEY_SPACE
   ; keyb-p/K-CR    Keyboard/KEY_CR
   ; keyb-p/FK-1    Keyboard/F_1
   ; keyb-p/FK-2    Keyboard/F_2
   ; keyb-p/FK-3    Keyboard/F_3
   ; keyb-p/FK-4    Keyboard/F_4
   ; keyb-p/FK-5    Keyboard/F_5
   ; keyb-p/FK-6    Keyboard/F_6
   ; keyb-p/FK-7    Keyboard/F_7
   ; keyb-p/FK-8    Keyboard/F_8
   ; keyb-p/FK-9    Keyboard/F_9
   keyb-p/K-A     Keyboard/KEY_A
   keyb-p/K-0     Keyboard/KEY_0
   keyb-p/K-1     Keyboard/KEY_1
   keyb-p/K-2     Keyboard/KEY_2
   keyb-p/K-3     Keyboard/KEY_3
   keyb-p/K-4     Keyboard/KEY_4
   keyb-p/K-5     Keyboard/KEY_5
   keyb-p/K-6     Keyboard/KEY_6
   keyb-p/K-7     Keyboard/KEY_7
   keyb-p/K-8     Keyboard/KEY_8
   keyb-p/K-9     Keyboard/KEY_9})

(def lwjgl->cloj
  (->>
    cloj->lwjgl
    (map reverse)
    (mapv vec)
    (vec)
    (into {})))

(defn read-keyboard! [keyb]
  (do
    (println "reading")

    (keyb-p/update! keyb)

    (doseq [[k v] lwjgl->cloj]
      (let [lwjgl-key-down (Keyboard/isKeyDown k)]

        (when lwjgl-key-down
          (println "KEY!")
          )
        (keyb-p/update-key! keyb v lwjgl-key-down)))))

(defn mk-keyboard []
  (let [keyb (keyb-p/default-kb-handler)]
    (do
      (keyb-p/init! keyb)

      (reify
        keyb-p/IKeyboardReader
        (get-key-states [_]
          (keyb-p/get-key-states keyb))

        (get-key-state [_ k]
          (keyb-p/get-key-state keyb k) )

        keyb-p/IKeyboardSystem
        (update! [_]
          (println "About to read!")
          (read-keyboard! keyb)) 

        (init! [_]
          (keyb-p/init! keyb)))
      )
    )
  )
