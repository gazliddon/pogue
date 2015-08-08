(ns experiments.chan
  (:require
    [cloj.totransit :as totransit :refer [read-transit-str]]
    [cloj.asyncutils :refer [alters! test-aysnc]]
    [clojure.pprint :as pp :refer [pprint]]
    [clojure.core.async  :as async ]))

(defn chan-atom 
  ([ch default xf]
   (let [val (atom default)]
     (do
       (async/go-loop []
         (let [new-val (async/<! ch)]
           (reset! val new-val)
           (recur)))

       (reify
         clojure.lang.IDeref
         (deref [_]
           (xf @val))))))

  ([ch default]
   (chan-atom ch default identity)
   ))

(defprotocol IUnrealize
  (unrealize! [_]))

(defn deferred-fn [func]
  (let [deffered-val (atom nil)]
    (reify
      IUnrealize
      (unrealize! [_]
        (reset! deffered-val nil))

      clojure.lang.IDeref
      (deref [_]
        (when (nil? @deffered-val)
          (reset! deffered-val (func)))
        @deffered-val
        ))))

(defmacro deffered [& forms]
  `(do
     (deferred-fn (fn []
                    (do ~@forms)))))

(defn make-stuff [event-chan func]
  (let [vl (atom nil)
        stuff-ret (deffered (func @vl)) ]
    (async/go-loop []
      (let [ v (async/<! event-chan)]
        (reset! vl v )
        (unrealize! stuff-ret)
        (recur)))

    stuff-ret)
  )






