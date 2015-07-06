(ns cloj.math.protocols)

(defprotocol IMathOps
  (add [_ b & rst])
  (sub [_ b & rst])
  (mul [_ b & rst])
  (div [_ b & rst]))
