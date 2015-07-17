(ns cloj.utils
  (:require
    [clojure.set :refer [difference]])

  (:require
    [clojure.core.async        :as async :refer [go <!]])

  #?(:cljs (:require
             [goog.string :as gstring]
             [goog.string.format :as gformat]))
  )

#?(:clj
   (do
     (defmacro with-clj [& forms]
       `(do
          ~@forms))

     (defmacro with-cljs [& forms]
       `(comment
          ~@forms)))
   :cljs
   (do
     (defmacro with-cljs [& forms]
       `(do
         ~@forms 
         )
       )
     (defmacro with-clj [& forms]
       `(comment
          ~@forms))))

(defn throw-err [e]
  (when (instance? Throwable e) (throw e))
  e)

(with-clj
  (defmacro <?? [ch]
    `(throw-err (async/<!! ~ch)))

  (defmacro <? [ch]
    `(throw-err (async/<! ~ch)))
  )

(defn map-difference
  [orig other]
  (let [changed (difference (set orig) (set other))
        added (difference (set (keys other)) (set (keys orig))) ]

    {:changed (->
                (fn [r [k v] ]
                  (assoc r k {:old v :new (k other)}))
                (reduce {} changed))
     :added (->>
              (map (fn [k] [k (k other)]) added) 
              (into {}))
     }))


