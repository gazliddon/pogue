(ns game.game
  )

(defprotocol IGameUpdate
  (game-update [_ dt]))

(defprotocol IGameInit
  (game-init [_]))

(defprotocol IGameClose
  (game-close [_]))

(defprotocol IGameRender
  (game-render [_]))

(defn make-game
  "make a game that satisfies all game interfaces
  adds default funcs for unsatisfied instances"
  [game]
  (reify
    IGameInit
    (game-init [_]
      (if (satisfies? IGameInit game)
        (game-init game)
        game))

    IGameUpdate
    (game-update [_ dt]
      (if (satisfies? IGameUpdate game)
        (game-update game dt)
        game) )

    IGameRender
    (game-render [_]
      (if (satisfies? IGameRender game)
        (game-render game)
        nil))
    
    IGameClose
    (game-close [_]
      (if (satisfies? IGameClose game)
        (game-close game)
        game))))

