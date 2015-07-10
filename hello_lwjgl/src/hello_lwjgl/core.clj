(ns hello-lwjgl.core
  (:require [hello-lwjgl.alpha :as alpha]
            [hello-lwjgl.beta  :as beta]
            [hello-lwjgl.gamma :as gamma]
            [hello-lwjgl.delta :as delta]
            [clojure.java.io :refer [file output-stream input-stream]]
            [clojure.core.async :as async :refer [chan >! <! go]]
            )
  (:import (org.lwjgl Sys))
  (:gen-class))

;; ======================================================================
(defn -main
  [& args]
  (println "Hello, Lightweight Java Game Library! V" (Sys/getVersion))
  (cond
   (= "alpha" (first args)) (alpha/main)
   (= "beta"  (first args)) (beta/main)
   (= "gamma" (first args)) (gamma/main)
   (= "delta" (first args)) (delta/main)
   :else (alpha/main)))

(defn blocking-load [file-name]
  (let [in-file (file file-name)
        size (.length in-file) ]
    (with-open [in (input-stream in-file) ]
      (let [ buf (byte-array size)
            n (.read in buf)
            ]
        buf
        ))))

(defn future-load [ret-chan file-name]
  (future
    (do
      (let [data (blocking-load "blocks.png")]
        (print "loaded " data)
        (put! ret-chan data)
        (print "sent ")
        data))))

(defn do-load [file-name]
  (let [ret-chan (chan)
        loader (future-load ret-chan file-name) ]
    ret-chan))

(defn tester []
  (do
    (let [load-chan (do-load "blocks.png")]
      (go
        (->>
          (<! load-chan)
          (println "got it"))))
    (println "should print first"))  )


