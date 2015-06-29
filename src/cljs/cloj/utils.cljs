(ns cloj.utils
  (:require
    [clojure.set :refer [difference]]
    [goog.string :as gstring]
    [goog.string.format :as gformat])
  )

;; TODO: Should be in a cljs specfic lib

(defn format [ arg & args] (apply gstring/format arg args) )

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


