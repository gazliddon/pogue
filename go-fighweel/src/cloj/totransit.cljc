(ns cloj.totransit
  (:require 
    [cognitect.transit :as transit]
    [msgpack.core :as msg])

  (:import 
    (java.io ByteArrayInputStream ByteArrayOutputStream ))
  )

(defn read-transit-str
  ([^String s file-type]
   (->
     (.getBytes s)
     (ByteArrayInputStream. )
     (transit/reader file-type)
     (transit/read )))
  ([^String s]
   (read-transit-str s :json)))
