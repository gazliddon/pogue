(ns gaz.rendermac)

(defmacro box [x y w h col]
  `(fn [r#]
    (box! r# [x y w h col]) ))

(defmacro display-list [commands]
  `(list
     ~@commands
    )
  )

