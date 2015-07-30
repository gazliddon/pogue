(ns
  "kind of like delay but also dependent on
   a vector of args.
   
   delay is defonce, so this is for dependencies
   that can change on restarting game.
   
   Will be used with the current gl context
   as a dependency"
  
  experiments.depdelay)

(defn mk-dep-delay [fn depsv]
  (let [mem (memoize fn depsv) ]
    (reify
      clojure.lang.IDeref 
      (deref [_] (@mem depsv)))))

(defmacro dep-delay [depsv forms]
  `(mk-dep-delay
     (fn [~@depsv]
       ~@forms
       )
     depsv))


