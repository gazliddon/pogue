(ns game.tilemapprotocol)

(defprotocol ITileMap
  (get-tile-data [_ tile-id])
  (get-width     [_])
  (get-height    [_])
  (reducer       [_ f memo-init])
  (get-tile      [_ x y])
  (set-tile      [_ x y v])
  (fill          [_ x y w h v]))
