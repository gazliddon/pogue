(ns cloj.jvm.loader
  (:require
    [digest :as digest]
    [cloj.protocols.loader :as loader-p]
    [clojure.core.async :as async :refer [chan >! <! put! go]]
    [clojure.java.io :refer [file output-stream input-stream]]))

(defrecord LoadedFile [data size file-name digest])

(defn mk-loader []
  (reify
    loader-p/ILoader
    (load-blocking! [_ file-name]
      (let [f (file file-name)
            size (.length f) ]
        (with-open [in (input-stream f)]
          (let [buf (byte-array size)
                arr (.read in buf)]
            (->LoadedFile buf size file-name (digest/sha-256 f))))))

    (load-async! [this file-name]
      (go
        (loader-p/load-blocking! this file-name)))))
