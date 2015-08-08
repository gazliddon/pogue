(ns cloj.jvm.watcher
  (:require
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
    (string/replace file-name pt "$1")))


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
                (do
                  (reset! watches (add-watcher file ch))

                  (when (file-exists? (str dir "/" file))
                    (rw-proto/on-change! this file))

                  ch))

              (on-change! [_ file]
                (let [no-dir (strip-dir dir file)]
                  (if-let [watchers (get-watchers no-dir)]
                    (let [data (slurp file)]
                      (doseq [ch watchers]
                        (async/put! ch data)))))))
        ]
    (reset! watcher ret)))



(comment

  (def watch-dir "resources/public/generated")

  (defn test-it []
    (let [w (dir-watcher! watch-dir true)]
      (async/go-loop [ch (rw-proto/watch! w "quad.json" (async/chan))]
        (let [v (async/<! ch)]
          (println "changed!")
          (rw-proto/stop! w)))
      ))
  )





