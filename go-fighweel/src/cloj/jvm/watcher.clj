(ns cloj.jvm.watcher
  (:require
    [experiments.chan :as exp]
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




(def watch-dir "resources/public/generated")

(defn test-it []
  (let [w (dir-watcher! watch-dir true)]
    (async/go-loop [ch (rw-proto/watch! w "quad.json" (async/chan))]
      (let [v (async/<! ch)]
        (println "changed!")
        (rw-proto/stop! w)))
    ))

(comment
  (do
    (def watch-dir "resources/public/generated")
    (def dir-watcher (dir-watcher! watch-dir true))

    (def model-load-chan (async/chan 1 (map read-transit-str)))
    (def gl-context-chan (async/chan))

    (defprotocol IOgl
      (make-model! [_ model]))

    (def gl-proxy
      (reify
        IOgl
        (make-model! [_ model]
          (println "I would be making a vao for ")
          (println model)
          #_(make-other-buffers model))))

    (defn mk-model
      [ogl-chan resource-chan]
      (let [res {:ogl nil :resource nil}

            deffer (deferred
                     (let [{:keys [ogl res]}]
                       (if (and ogl res)
                         :we-have-everything
                         nil))) ]

        (async/go-loop []
          (let [[v port] (async/alts! [ogl-chan resource-chan]) ]
            (condp = port
              resource-chan (swap! res assoc :resource v)
              ogl-chan (swap! res assoc :ogl v))
            (exp/unrealize! deffer)
            (recur)))

        deffer)
      )

    (def model-ref (mk-model gl-context-chan model-load-chan))

    (rw-proto/watch! dir-watcher "quad.json" model-load-chan)

    (println @model-ref)
    (async/put! gl-context-chan :woo)
    )
  
  )






