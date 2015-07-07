(ns async.core
  (:require
    [digest :as digest]
    [clojure.core.async :as async :refer [chan >! <! put!]]
    [clojure.java.io :refer [file output-stream input-stream]])) 

(defrecord LoadedFile [data size file-name digest])

(defn load-blocking [file-name]
  (let [f (file file-name)
        size (.length f) ]
    (with-open [in (input-stream f)]
      (let [buf (byte-array size)
            arr (.read in buf)]
        (->LoadedFile buf size file-name (digest/sha-256 f))
        ))))

(defn load-async
  ([ret-chan file-name]
   (future
     (let [file (load-blocking file-name)]
       (put! ret-chan file)))
   ret-chan )

  ([file-name]
   (load-async (chan) file-name)))

