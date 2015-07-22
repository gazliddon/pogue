(ns cloj.math.protocols
  (:refer-clojure :exclude [min max]))

(defprotocol IMathOps
  (min [_ b])
  (max [_ b])
  (add [_ b])
  (sub [_ b])
  (mul [_ b])
  (div [_ b]))

(extend-type java.lang.Number
  IMathOps
  (min [a b] (clojure.core/min a b))
  (max [a b] (clojure.core/max a b))
  (add [a b] (+ a b))
  (sub [a b] (- a b))
  (mul [a b] (* a b))
  (div [a b] (/ a b)))

