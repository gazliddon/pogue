(ns game.gamekeys
  (:require 
    [cloj.protocols.keyboard  :as key-p]))

(defprotocol IGameKeys
  (update! [_])
  (quit?   [_])
  (up?     [_])
  (down?   [_])
  (left?   [_])
  (right?  [_])
  (fire?   [_]))

(defn any-keys-pressed? [keypfn ks]
  (reduce (fn [r v]
            (or r (keypfn v))) false ks))

(defn mk-game-keys [keyb key-defs]
  (let [any-pressed? (fn [kk]
                       (any-keys-pressed?
                         #(:state (key-p/get-key-state keyb %) )
                         (kk key-defs)))]
    (reify
      IGameKeys

      (update![_] (key-p/update! keyb))

      (quit?  [_] (any-pressed? :quit))
      (up?    [_] (any-pressed? :up))
      (down?  [_] (any-pressed? :down))
      (left?  [_] (any-pressed? :left))
      (right? [_] (any-pressed? :right))
      (fire?  [_] (any-pressed? :fire))
      ))
  )

