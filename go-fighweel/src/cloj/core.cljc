(ns cloj.core
  )


(do
(defn diff-2 [^long n]
  (*
   (/ 1 12)
   (- n 1)
   n
   (+ n 1)
   (+ (* 3 n) 2)))  )


(comment
  ;; Mess around with pub / sub
  ;; seems good

  (do
    (def in-chan (chan 10000))

    (def pubber (pub in-chan (fn [[t v]]
                               (println "topic" t)
                               t)))

    (def shoes-chan (chan))
    (sub pubber :shoes shoes-chan)

    (put! in-chan [:shoes "val"] )

    (println (<!! shoes-chan))
    ))




