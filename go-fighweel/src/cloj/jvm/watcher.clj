(defprotocol IResourceWatcher
  (stop! [_])
  (watch! [_ sub-resource ch])
  (on-change! [_ file]))

(defn mk-dir-watcher [dir recursive on-change]
  (start-watch
    [{:path dir
      :event-types [:modify :create]
      :bootstrap (fn [path] (println "Starting to watch " path))
      :callback (fn [_ file]
                  (on-change file))
      :options {:recursive recursive}}]))


(defn dir-watcher [dir recursive]
  (let [watcher (atom nil)
        watches (atom {})
        close-watch  (mk-dir-watcher
                       dir
                       recursive
                       #(when @watcher
                          (on-change! @watcher %)))

        get-watchers (fn [file] (get @watches file ()))

        add-watcher  (fn [file ch]
                       (->>
                         (cons ch (get-watchers file))
                         (assoc @watches file)))
        ret (reify
              IResourceWatcher
              (stop! [_]
                (close-watch)
                (reset! watches nil))

              (watch! [_ file ch]
                (reset! watches (add-watcher file ch)))

              (on-change! [_ file]
                (if-let [watchers (get-watchers file)]
                  (let [data (slurp (str dir "/" file))]
                    (doseq [ch watchers]
                      (async/put! ch data)))))) ]
    (reset! watcher ret)))


(defn test-it []
  (do
    (doseq [w @all-watchers]
      (w))
    (reset! all-watchers ())
    (let [watcher (file watched-dir false)]
      (do
        (swap! all-watchers conj watcher)
        (watch! watcher "quad.json" (async/chan))
        (loop []
          (Thread/sleep 3000)
          (println "slept")
          (recur)
          ))))

  )


