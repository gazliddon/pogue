(ns cloj.jvm.watcher
  (:require
    [experiments.chan :as exp :refer [deffered ]]
    [cloj.lwjgl.model :as model :refer [make-other-buffers]]
    [cloj.totransit :refer [read-transit-str]]
    [clojure.core.async :as async]

    [cloj.protocols.resourcewatcher :as rw-proto]
    [clojure.core.async :as async]
    [clojure-watch.core :as watch]
    [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Split out into own utils file
(defn escape-for-regex [ s ]
  (java.util.regex.Pattern/quote s))

(defn strip-dir [base-dir file-name]
  (let [re (re-pattern (str "^" (escape-for-regex base-dir) "\\/(.*)$"))]
    (string/replace file-name re "$1")))


(defn file-exists? [file]
  (.exists (clojure.java.io/as-file file)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-dir-watcher [dir recursive on-change]
  (watch/start-watch
    [{:path dir
      :event-types [:modify :create]
      :bootstrap (fn [path] (println "Starting to watch " path))
      :callback (fn [_ file]
                  (on-change file))
      :options {:recursive recursive}}]))

(defn dir-watcher! [dir recursive]
  (let [watcher (atom nil)
        watches (atom {})
        close-watch  (mk-dir-watcher
                       dir
                       recursive
                       #(when @watcher
                          (rw-proto/on-change! @watcher %)))

        get-watchers (fn [file] (get @watches file ()))

        add-watcher  (fn [file ch]
                       (->>
                         (cons ch (get-watchers file))
                         (assoc @watches file)))
        ret (reify
              rw-proto/IResourceWatcher
              (stop! [_]
                (close-watch)
                (reset! watches nil))

              (watch! [this file ch]
                (let [full-file-name (str dir "/" file)]
                  (do
                    (reset! watches (add-watcher file ch))
                    (when (file-exists? full-file-name)
                      (rw-proto/on-change! this full-file-name))

                    ch)))

              (on-change! [_ file]
                (when-let [watchers (get-watchers (strip-dir dir file))]
                  (let [data (slurp file)]
                    (doseq [ch watchers]
                      (async/put! ch data))))))
        ]
    (reset! watcher ret)))


(defprotocol IOgl
  (make-model! [_ model]))

(def gl-proxy
  (reify
    IOgl
    (make-model! [_ model]
      (make-other-buffers model))))

(defn mk-model

  "this can be genericised from making a model
   and making a resource in general
   the two vals are the dynamically changing data (file on disk)
   and the resource maker (ogl context?)"

  [ogl-chan resource-chan]
  (let [res (atom  {:ogl nil :resource nil})
        deffer (deffered
                 (let [{:keys [ogl resource]} @res]
                   (when (and ogl resource)
                     (make-model! ogl resource))))]

    (async/go-loop []
      (let [[v port] (async/alts! [ogl-chan resource-chan]) ]
        (do 
          (condp = port
            resource-chan (swap! res assoc :resource v)
            ogl-chan (swap! res assoc :ogl v))
          (exp/unrealize! deffer)
          (recur))))

    deffer))

(def watch-dir "resources/public/generated")
(def model-load-chan (async/chan 1 (map read-transit-str)))
(def gl-context-chan (async/chan))

(def dir-watcher (deffered  
                   (dir-watcher! watch-dir true)))

(defn mk-watched-model [file-name]
  (let [ret-val (mk-model gl-context-chan model-load-chan) ]
    (do
      (rw-proto/watch! @dir-watcher file-name model-load-chan)
      (async/>!! gl-context-chan gl-proxy)
      ret-val)))








