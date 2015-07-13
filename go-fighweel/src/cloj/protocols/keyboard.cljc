(ns cloj.protocols.keyboard)

(defrecord Key [name id])

;; Virtual key defs, up to the host to tie these
;; to physical keys

(def key-map
  {:key-a      {:string "a"}
   :key-0      {:string "0"}
   :key-1      {:string "1"}
   :key-2      {:string "2"}
   :key-3      {:string "3"}
   :key-4      {:string "4"}
   :key-5      {:string "5"}
   :key-6      {:string "6"}
   :key-7      {:string "7"}
   :key-8      {:string "8"}
   :key-9      {:string "9"}

   :key-f1      {:string "function key 1"}
   :key-f2      {:string "function key 2"}
   :key-f3      {:string "function key 3"}
   :key-f4      {:string "function key 4"}
   :key-f5      {:string "function key 5"}
   :key-f6      {:string "function key 6"}
   :key-f7      {:string "function key 7"}
   :key-f8      {:string "function key 8"}
   :key-f9      {:string "function key 9"}

   :key-up     {:string "cursor up"}
   :key-down   {:string "cursor down"}
   :key-left   {:string "cursor left"}
   :key-right  {:string "cursor right"}
   :key-space  {:string "space"}
   :key-cr     {:string "carriage return"} } )

(defprotocol IKeyboardSystem
  (init! [_])
  (update-key! [_ k state])
  (update! [_]))

(defprotocol IKeyboardReader
  (get-key-state [_ k])
  (get-key-states [_])
  )

(defrecord KeyState [state last-state pressed released])

(def default-key-state (KeyState. false false false false))

(defn my-get-key-state [key-states key-code]
  (get key-states key-code default-key-state))

(defn generate-new-key-record [new-state {:keys [state last-state pressed released]}]
  (KeyState. new-state state (and (not state) new-state) (and state (not new-state))))

(defn update-key [key-states key-code state ]
  (let [current-record (my-get-key-state key-states key-code)
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
      IKeyboardSystem
      (init! [_] (reset! key-atom {}))

      (update-key! [_ key-code state]
        (reset! key-atom (update-key @key-atom key-code state) ))

      (update! [_]
        (reset! key-atom (update-keys @key-atom))) 

      IKeyboardReader
      (get-key-state [_ key-code]
        (my-get-key-state @key-atom key-code))

      (get-key-states [_]
        @key-atom))))

