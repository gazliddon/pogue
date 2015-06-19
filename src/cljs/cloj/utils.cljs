(ns cloj.utils
  (:require
    [goog.string :as gstring]
    [goog.string.format :as gformat])
  )

;; TODO: Should be in a cljs specfic lib

(defn format [ arg & args] (apply gstring/format arg args) )

