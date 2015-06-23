(ns game.sprdata
  )

(defn spr16
  ([x y w h] (mapv #(* 16 %) [x y w h]))
  ([x y] (spr16 x y 1 1))
  ([n] (spr16 (mod n 16) (int (/ n 16)))))


(defn spr32
  ([x y] (mapv #(* 32 %) [x y 32 32])))


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
            :wacdonalds   (spr16 0 2)}

   :bubbob {:bub0 (spr32 0 0)
            :bub1 (spr32 0 1)
            :bub2 (spr32 0 2)
            :bub3 (spr32 0 3)
            :bub4 (spr32 0 4)
            :bub5 (spr32 0 5)
            :bub6 (spr32 0 6)
            :bub7 (spr32 0 7)
            }

   })

