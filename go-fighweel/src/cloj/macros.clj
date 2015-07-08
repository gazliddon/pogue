(ns cloj.macros
  (:require
    [clojure.walk :as walk]))

(def op-template `(:op (:elem :arg0) (:elem :arg1)) ) 

(def fn-template `(defn :func
                    ([:arg0 :arg1]
                     (let :let-map
                       :ret-val))
                    
                    ([:arg0 :arg1 & :argn]
                     (let :let-map
                       (if :argn
                         (apply :func :ret-val :argn)
                         :ret-val)))) )

(def ret-template `(if :argn (:func :ret-val (first :argn)) :ret-val ))

(defn mk-elem-op [op arg0-sym arg1-sym elem]
  (walk/prewalk-replace {:op op :elem elem :arg0 arg0-sym :arg1 arg1-sym } op-template))

(defn genksym [k]
  (gensym (str (name k) "-")))

(defn mk-replacements [func op arg0-sym arg1-sym argn-sym & elems]
  (let [ret-sym (gensym "ret-")
        sym-map (map (fn [e] {:esym (genksym e) :elem e}) elems)

        sym-map-fn (fn [f] (mapcat (fn [{:keys [esym elem]}] (f esym elem)) sym-map))

        ret-val (->> (sym-map-fn
                       (fn [esym elem]
                         (list elem esym)))
                     (flatten)
                     (cons `hash-map)) 

        let-map (->
                  (sym-map-fn
                    (fn [esym elem]
                      (list esym (mk-elem-op op arg0-sym arg1-sym elem))))
                  (concat (list ret-sym ret-val) )

                  (vec))
        ]
    {:let-map let-map
     :ret-val ret-sym
     :op      op
     :func    func
     :arg0    arg0-sym
     :arg1    arg1-sym
     :argn    argn-sym
     }))

(defmacro mk-vec-op [nm op & elems]
  (->
    (apply mk-replacements nm op (gensym "arg0-") (gensym "arg1-") (gensym "argn-") elems)
    (walk/prewalk-replace fn-template)))
#?(:cljs 
    (defmacro import-vars [[_quote ns]]
      `(do
         ~@(->>
             (cljs.analyzer.api/ns-interns ns)
             (remove (comp :macro second))
             (map (fn [[k# _]]
                    `(def ~(symbol k#) ~(symbol (name ns) (name k#))))))))
     )



