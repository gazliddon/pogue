(ns cloj.lwjgl.main

  (:require
    [cloj.lwjgl.system :as lwjgl-sys]
    [cloj.protocols.system :as sys]
    [clojure.core.async :as async :refer [chan put! <! go-loop]]  
    [cloj.core :refer [quit? main]])
  )

(def sys (lwjgl-sys/mk-system))
(def msg-ch (sys/get-msg-chan sys))

(defn tmain []
  (future
    (main sys)))

