(ns
  ; "kind of like delay but also dependent on
  ;  a vector of args.
   
  ;  delay is defonce, so this is for dependencies
  ;  that can change on restarting game.
   
  ;  Will be used with the current gl context
  ;  as a dependency"
  
  experiments.depdelay)

(defn- mk-dep-delay [func global-atom]
  (let [atom-val (atom @global-atom) 
        func-rez (atom nil) ]
    (reify
      clojure.lang.IDeref 
      (deref [_]
        (when (or
                (not= @global-atom @atom-val)
                (nil? @func-rez))
          (reset! func-rez (func))
          (reset! atom-val @global-atom)
          )
        @func-rez
        ))))

(defmacro dep-delay [global-atom & forms]
  `(mk-dep-delay
     (fn []
       (do
         ~@forms
         )
       )
     ~global-atom
     ))


(def gl-context (atom nil))


(def gl-resources (atom {:textures {}
                         :render-buffers {}
                         :vao  {}
                         }))

(defprotocol IUnrealize
  (unrealize! [_])
  (get-val [_])
  )

(defn mk-rez-atom! [func]
  (let [realized (atom nil)]
    (reify
      IUnrealize
      (unrealize! [_]

        (get-val [_] @realzed)
        (reset! realized nil))

      clojure.lang.IDeref
      (deref [this]
        (when (nil? @realized)
          (reset! realized (func)))
        @realized
        ))
    ))

(defn add-gl-resource! [korks func]
  (do
    (let [rez-atom (mk-rez-atom! func)]
      (remove-watch gl-context [korks])
      (add-watch gl-context
                 korks
                 (fn [_ _ _ _]
                   (unrealize! rez-atom)))
      (swap!
        gl-resources
        assoc-in korks rez-atom)
      rez-atom))) 
(do
  (def x


    (add-gl-resource! [:textures :tex-1]
                      (fn []
                        (println "Created!")
                        )
                      ))
  (println x)

  )






