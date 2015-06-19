(ns cloj.utils
  (:require
    [goog.string :as gstring]
    [goog.string.format :as gformat]
    )
  )

(defn format [ arg & args] (apply gstring/format arg args) )

