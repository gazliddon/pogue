(ns cloj.keyboard)

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

