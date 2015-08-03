(ns cloj.asyncutils
  (:require
    [clojure.core.async :as async])
  
  )

(defn alters!

  "Takes a map of {:key1 chan1 :key2 chan2}
   and returns an output channel

   if you put a value to a map is sent to the
   output with the value for the key the channel
   was sent on replaced with the value

   The other values in the map are the last value
   that was sent to that channel or nil if a value
   has never been sent.

   (def ch-map {:chan-a (chan) :chan-b (chan))
   (def out-chan (alters! ch-map))
   (put! (:chan-a) 'hello!')
   (<!! out-chan)
   {:chan-a 'hello'
    :chan-b nil}

   (put! (:chan-b) 'there')
   (<!! out-chan)
   {:chan-a 'hello'
    :chan-b nil} "
  
  [ chan-map ]
  (let [ret-chan (async/chan)
        chans (vals chan-map)
        init-mp (into {} (map (fn [[v _]] [v nil]) chan-map))
        rev-map (into {} (map vector (vals chan-map) (keys chan-map))) ]
    (async/go-loop [mp init-mp]
      (let [ [v port] (async/alts! chans)
            new-map (assoc mp (get rev-map port) v) ]
        ;; Should this be put! ?
        (async/>! ret-chan new-map)
        (recur new-map) ))

    ret-chan))

(defn test-aysnc
  "takes from a chan forever and prints the result.
   Handy for debugging core.async in the repl"
  [ch]
  (do
   (loop [cnt 0]
    (let [v (async/<!! ch)]
      (println "step: " cnt)
      (pprint v)
      (println)
      )
    (recur (inc cnt))
    )))

