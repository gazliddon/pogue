(ns gaz.tilemapprotocol)

(defprotocol ITileMap
  (get-width [_])
  (get-height [_])
  (reducer [_ f memo-init])
  (get-tile [_ x y])
  (set-tile [_ x y v])
  (fill [_ x y w h v]))
