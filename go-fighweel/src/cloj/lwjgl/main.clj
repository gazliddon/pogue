(ns cloj.lwjgl.main

  (:require
    [cloj.lwjgl.system :as lwjgl-sys]
    [cloj.protocols.system :as sys]
    [clojure.core.async :as async :refer [chan put! <! go-loop]]  
    [cloj.core :refer [main]]))

(def sys (lwjgl-sys/mk-system))

(defn tmain []
  (future
    (try
      (main sys) 
      (catch Exception e
        (println "Error: " (.getMessage e)))))

  "launched")

