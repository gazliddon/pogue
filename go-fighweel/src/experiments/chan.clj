(ns experiments.chan
  (:require
    [clojure.pprint :as pp :refer [pprint]]
    [ clojure.core.async  :as async :refer [go
                                            <!
                                            >!
                                            <!!
                                            >!!
                                            go-loop
                                            chan
                                            put!
                                            timeout
                                            alts!
                                            ]]))

; (defn chan-atom 
;   ([ch default on-change]
;    (let [val (atom default)]
;      (do
;        (go-loop []
;          (let [new-val (<! ch)]
;            (on-change new-val @val)
;            (reset! val new-val)))

;        (reify
;          clojure.lang.IDeref
;          (deref [_]
;            @val)))))

;   ([chan default]
;    (chan-atom chan default (fn [_ _]))))

; (defn nil-map [mp]
;   (into {} (map vector (keys mp) (repeat nil))))

; (defn inv-map [mp]
;   (into {} (map vector (vals mp) (keys mp))))

; (defn combine [mp]
;   (let [inverse (inv-map mp) 
;         chans (vals mp)]
;     )
;   )

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
  (let [ret-chan (chan)
        chans (vals chan-map)
        init-mp (into {} (map (fn [[v _]] [v nil]) chan-map))
        rev-map (into {} (map vector (vals chan-map) (keys chan-map))) ]
    (go-loop [mp init-mp]
      (let [ [v port] (async/alts! chans)
            new-map (assoc mp (get rev-map port) v) ]
        (>! ret-chan new-map)
        (recur new-map) ))

    ret-chan))

(defn test-aysnc
  "takes from a chan forever and prints the result.
   Handy for debugging core.async in the repl"
  [ch]
  (do
   (loop [cnt 0]
    (let [v (<!! ch)]
      (println "step: " cnt)
      (pprint v)
      (println)
      )
    (recur (inc cnt))
    )))

