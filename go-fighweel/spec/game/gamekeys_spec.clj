(ns game.gamekeys-spec
  (:require [speclj.core :refer :all]
            [game.gamekeys :as gamek]))


(defn escape-kfun
  "Returns true if esc key is polled"
  [k]
  (= k :key-esc))


(describe "Game keyboard prims"

  (it "shouldn't report true with not esc key"
    (should-not 
      (gamek/any-keys-pressed? escape-kfun [:key-a :key-b :key-c])))

  (it "should find the escape key in this vector"
    (should
      (gamek/any-keys-pressed? escape-kfun [:key-a :key-b :key-esc :key-c])  ))
  )

(run-specs)

