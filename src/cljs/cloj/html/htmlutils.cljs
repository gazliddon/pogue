(ns cloj.html.utils
  )

(defn get-elem-dims [elem]
  [(.-width elem)
   (.-height elem) ])

(defn set-elem-dims! [elem {w :x h :y}]
  (doto elem
    (aset "width" w)
    (aset "height" h)  ))

(defn get-window-dims []
  [(.-innerWidth js/window)
   (.-innerHeight js/window) ] )
