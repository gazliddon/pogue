(ns cloj.macros
  (:require
    [clojure.walk :as walk]))

(def op-template `(:op (:elem :arg0) (:elem :arg1)) ) 
(def fn-template `(defn :func [:arg0 :arg1] (let :let-map :ret-val)))

(defn mk-elem-op [op arg0-sym arg1-sym elem]
  (walk/prewalk-replace {:op op :elem elem :arg0 arg0-sym :arg1 arg1-sym } op-template))

(defn mk-replacements [func op arg0-sym arg1-sym & elems]
  (let [sym-map (map (fn [e] {:esym (gensym) :elem e}) elems)

        sym-map-fn (fn [f] (mapcat (fn [{:keys [esym elem]}] (f esym elem)) sym-map))

        let-map (->> (sym-map-fn
                       (fn [esym elem]
                         (list esym (mk-elem-op op arg0-sym arg1-sym elem))))
                     (vec))

        ret-map (->> (sym-map-fn
                       (fn [esym elem]
                         (list elem esym)))
                     (flatten)
                     (cons `hash-map))]
    {:let-map let-map
     :ret-val ret-map
     :op      op
     :func    func
     :arg0    arg0-sym
     :arg1    arg1-sym}))

(defmacro mk-vec-op [nm op & elems]
  (->
    (apply mk-replacements nm op (gensym) (gensym) elems)
    (walk/prewalk-replace fn-template)))

(defmacro import-vars-2 [[_quote ns]]
  `(do
     ~@(->>
         (cljs.analyzer.api/ns-interns ns)
         (remove (comp :macro second))
         (map (fn [[k# _]]
                `(def ~(symbol k#) ~(symbol (name ns) (name k#))))))))

