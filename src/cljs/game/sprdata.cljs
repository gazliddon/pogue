(ns game.sprdata
  )

(defn spr16
  ([x y w h] (mapv #(* 16 %) [x y w h]))
  ([x y] (spr16 x y 1 1))
  ([n] (spr16 (mod n 16) (int (/ n 16)))))


(defn spr32
  ([x y] (mapv #(* 32 %) [x y 1 1])))


(def spr-data
  {:blocks {:b0      (spr16 0)
            :b1      (spr16 1)
            :b2      (spr16 2)
            :b-floor (spr16 3)
            :b4      (spr16 4)
            :b5      (spr16 5)
            :b6      (spr16 6)
            :b7      (spr16 7)}

   :items  {:green-pepper (spr16 3)
            :aubergine    (spr16 4)
            :carrot       (spr16 5)
            :onion        (spr16 6)
            :wacdonalds   (spr16 0 2)}

   :bubbob {:bub0 (spr32 0 0)
            :bub1 (spr32 1 0)
            :bub2 (spr32 2 0)
            :bub3 (spr32 3 0)
            :bub4 (spr32 4 0)
            :bub5 (spr32 5 0)
            :bub6 (spr32 6 0)
            :bub7 (spr32 7 0)}

   })

(defn anim [t s frms]
  (nth frms 
       (mod (int (/ t s)) (count frms))) )

(defn mk-anim-fn [ speed frames ]
  (fn [t]
    (anim t speed frames)))

(def anim-data
  {:bub-stand  (mk-anim-fn 0.1 [:bub0 :bub1 :bub2 :bub3])
   :bub-walk   (mk-anim-fn 0.1 [:bub0 :bub1 :bub2 :bub3])
   }
  )

