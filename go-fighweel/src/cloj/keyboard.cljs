(ns cloj.keyboard)


(defrecord Key [name])

;; Virtual key defs, up to the host to tie these
;; to physical keys

(def K-A     (->Key "a" ))

(def K-0     (->Key "0" ))
(def K-1     (->Key "1" ))
(def K-2     (->Key "2" ))
(def K-3     (->Key "3" ))
(def K-4     (->Key "4" ))
(def K-5     (->Key "5" ))
(def K-6     (->Key "6" ))
(def K-7     (->Key "7" ))
(def K-8     (->Key "8" ))
(def K-9     (->Key "9" ))

(def FK-1     (->Key "function key 1" ))
(def FK-2     (->Key "function key 2" ))
(def FK-3     (->Key "function key 3" ))
(def FK-4     (->Key "function key 4" ))
(def FK-5     (->Key "function key 5" ))
(def FK-6     (->Key "function key 6" ))
(def FK-7     (->Key "function key 7" ))
(def FK-8     (->Key "function key 8" ))
(def FK-9     (->Key "function key 9" ))

(def K-UP    (->Key "cursor up" ))
(def K-DOWN  (->Key "cursor down" ))
(def K-LEFT  (->Key "cursor left" ))
(def K-RIGHT (->Key "cursor right" ))
(def K-SPACE (->Key "space" ))
(def K-CR    (->Key "carriage return" ))

(defprotocol IKeyboard
  (init! [_])
  (update-key! [_ k state])
  (get-key-state [_ k])
  (get-key-states [_])
  (update! [_]))

(defrecord KeyState [state last-state pressed released])

(def default-key-state (KeyState. false false false false))

(defn get-key-state [key-states key-code]
  (get key-states key-code default-key-state))

(defn generate-new-key-record [new-state {:keys [state last-state pressed released]}]
  (KeyState. new-state state (and (not state) new-state) (and state (not new-state))))

(defn update-key [key-states key-code state ]
  (let [current-record (get-key-state key-states key-code)
        new-record (generate-new-key-record state current-record ) ]
    (if (not= (:state current-record ) state)
      (assoc key-states key-code new-record )
      key-states)))

(defn update-keys [key-states]
  (reduce
    (fn [res [k {:keys [state] :as v}]]
      (assoc res k (generate-new-key-record state v)))
    {}
    key-states))

(defn default-kb-handler []
  (let [key-atom (atom {})]
    (reify
      IKeyboard
      (init! [_] (reset! key-atom {}))

      (update-key! [_ key-code state]
        (reset! key-atom (update-key @key-atom key-code state) ))

      (get-key-state [_ key-code]
        (get-key-state @key-atom key-code))

      (get-key-states [_]
        @key-atom
        )

      (update! [_]
        (reset! key-atom (update-keys @key-atom))) 
      )))

