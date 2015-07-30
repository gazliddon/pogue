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

(comment
  (defonce x (atom 2))

  (def ddx
    (dep-delay x
      (println "called@it")
      "returning"))

  (println @ddx)

  (swap! x inc))





