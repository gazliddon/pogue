(ns experiments.depdelay
  ; "kind of like delay but also dependent on
  ;  a vector of args.
   
  ;  delay is defonce, so this is for dependencies
  ;  that can change on restarting game.
   
  ;  Will be used with the current gl context
  ;  as a dependency"

  (:require
    [ clojure.core.async  :as async :refer [go <! go-loop]]
    [ clojure-watch.core :refer [start-watch]] ))

(def gl-context (atom 0))

(def gl-resources (atom {:textures {}
                         :render-buffers {}
                         :vaos  {} }))

(defprotocol IUnrealize
  (unrealize! [_]))

(defn mk-rez-atom! [func]
  (let [realized (atom nil)]
    (reify
      IUnrealize
      (unrealize! [_]
        (reset! realized nil))

      clojure.lang.IDeref
      (deref [this]
        (when (nil? @realized)
          (reset! realized (func)))
        @realized))))

(defn add-gl-resource! [korks func]
  (let [rez-atom (mk-rez-atom! func)]
    (do
      (remove-watch gl-context [korks])
      (add-watch gl-context
                 korks
                 (fn [_ _ _ _]
                   (unrealize! rez-atom)))
      (swap! gl-resources assoc-in korks rez-atom)
      rez-atom)))

(defmacro gl-create-resource! [typ id & body]
  `(let [func# (fn [] (do ~@body))]
     (add-gl-resource! [~typ ~id] func#)))

(defmacro gl-create-texture! [id & body]
  `(gl-create-resource! :textures ~id ~@body))

(defmacro gl-create-vao! [id & body]
  `(gl-create-resource! :vaos ~id ~@body))

(defmacro gl-create-render-buffer! [id & body]
  `(gl-create-resource! :render-buffers ~id ~@body))

(defn update-gl-context! [ func]
  (swap! gl-context func))

(defn depends-on-file
  "make this resource depend on a file
   How do I watch files in clj then?"
  [korks file-name]
  (let [thing-to-update (get-in @gl-resources korks)]
    (when (not= nil thing-to-update)
      (start-watch [{:path file-name
                     :event-types [:modify]
                     :bootstrap (fn [path] (println "Starting to watch " path))
                     :callback (fn [_ _] (do
                                           (println "The file changed!")
                                           (unrealize! thing-to-update)))
                     :options {:recursive false}}]))
    )
  )

(defn chan-atom 
  ([chan default on-change]
   (let [val (atom default)]
     (do
       (go-loop []
         (let [new-val (<! chan)]
           (on-change new-val @val)
           (reset! val new-val)))

       (reify
         clojure.lang.IDeref
         (deref [_]
           @val)))))

  ([chan default]
   (chan-atom chan default (fn [_ _]))))


