(ns cloj.jvm.watcher
  (:require
    [ clojure.core.async  :as async :refer [go <! go-loop]]
    [ clojure-watch.core :refer [start-watch]]))

(def watched-dir "resources/public/generated")

(defprotocol IResourceWatcher
  (watch! [_ resource ch])
  (unwatch [_ resource ch] )
  (service-watches! [_ resource new-val])
  )

(defn mk-watcher [watch-pool]
  (reify
    IResourceWatcher

    (watch! [_ resoure ch]
      (let [current-watches (get @watch-pool resoure ())
            new-watches (conj current-watches ch) ]
        (swap! watch-pool assoc resoure new-watches)))

    (service-watches! [_ resource new-val]
      (doseq [ch (get @watch-pool resource)]
        (async/put! ch new-val)))))

(defn mk-dummy-watcher [watch-pool]
 (reify
    IResourceWatcher
    (watch! [_ resource ch]
      (println "being asked to watch" resource))

    (service-watches! [_ resource new-val]
      (println "this resource updated" resource)
      )) 
  )

(defn watch-dir! [dir recursive]
  (let [watch-pool (atom {})
        watcher (mk-dummy-watcher watch-pool)
        watch-fn (fn [evtpe file-name]
                   (service-watches! watcher file-name (slurp file-name))) ]

    (do
      (start-watch [{:path dir
                     :event-types [:modify :create]
                     :bootstrap (fn [path] (println "Starting to watch " path))
                     :callback watch-fn
                     :options {:recursive recursive}}])
      watcher)))

(defn test-it []
  (let [watcher (watch-dir! watched-dir false)]
    (do
      (watch! watcher "quad.json" (async/chan))
      (loop []
        (Thread/sleep 3000)
        (println "slept")
        (recur)
        ))))



