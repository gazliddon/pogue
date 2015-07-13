(ns cloj.lwjgl.keyboard
  (:require 
    [cloj.protocols.keyboard :as keyb-p]
    [cloj.lwjgl.kbdata :refer [cloj->lwjgl lwjgl->cloj]])
    (:import (org.lwjgl.input Keyboard)))

(defn read-keyboard! [keyb]
  (do
    (doseq [[k v] lwjgl->cloj]
      (let [lwjgl-key-down (Keyboard/isKeyDown k)]
        (keyb-p/update-key! keyb v lwjgl-key-down)))
    (keyb-p/update! keyb)))


(defn poll-read-keyboard [keyb]
  (do
    (doseq [[k v] lwjgl->cloj]
      (let [lwjgl-key-down (Keyboard/isKeyDown k)]
        (keyb-p/update-key! keyb v lwjgl-key-down)))
    (keyb-p/update! keyb)))

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
          (read-keyboard! keyb)) 

        (init! [_]
          (keyb-p/init! keyb)))
      )
    )
  )
