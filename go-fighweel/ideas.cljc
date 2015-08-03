(def chan-atom 
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

(defn nil-map [mp]
  (into {} (map vector (keys mp) (repeat nil))))

(defn inv-map [mp]
  (into {} (map vector (vals mp) (keys mp))))

(defn combine [mp]
  (let [inverse (inv-map mp) 
        chans (vals mp)]
    )
  )

(map vector '(1 2 3) '(4 5 6))

(def mp {:a 1 :b 2})
(inv-map mp)
(nil-map mp)


