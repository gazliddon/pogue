(ns cloj.html.utils
  )

(defn get-elem-dims [elem]
  [(.-innerWidth elem)
   (.-innerHeight elem) ])

(defn set-elem-dims! [elem {w :x h :y}]
  (println (str "setting dims to " w " " h))
  (doto elem
    (aset "innerWidth" w)
    (aset "innerHeight" h)  ))

(defn get-window-dims []
  (get-elem-dims js/window))

