(ns game.sprdata
  )

(defn spr16
  ([x y w h] (mapv #(* 16 %) [x y w h]))
  ([x y] (spr16 x y 1 1))
  ([n] (spr16 (mod n 16) (int (/ n 16)))))

(def spr-data
  {:blocks {:b0 (spr16 0)
            :b1 (spr16 1)
            :b2 (spr16 2)
            :b3 (spr16 3)
            :b4 (spr16 4)
            :b5 (spr16 5)
            :b6 (spr16 6)}

   :items  {:green-pepper (spr16 3)
            :aubergine    (spr16 4)
            :carrot       (spr16 5)
            :onion        (spr16 6)
            :wacdonalds   (spr16 0 2)
            }})

