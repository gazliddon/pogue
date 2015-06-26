(ns game.tiledata
  )

(defn- tiles-four [tile] (into [] (take 4 (repeat tile))))

(def duff-tile {:gfx (tiles-four :b-purple)})

(def tile-data {:floor { :gfx (tiles-four :b-floor ) }
                :blank { :gfx (tiles-four :b-blank ) }
                :door  { :gfx (tiles-four :b-wood-0) }})

