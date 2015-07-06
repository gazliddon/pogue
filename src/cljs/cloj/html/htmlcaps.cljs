(ns cloj.html.caps
  )

;; HTML capabilities - uses modernizr
;; 

(def caps
  {:has-game-pad  (.-gamepads js/Modernizr )
   })

