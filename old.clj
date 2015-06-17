
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; (def system ( mk-system "shit-div" "shit-canvas" ))

; (def rt-gaz
;   (-> (get-resource-manager system)
;       (create-render-target! "shit-canvas" 100 100)))

; (def im-gaz
;  (-> (get-resource-manager system)
;      (load-img! "shit-tiles")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; (defonce update-chan (chan))

; (def level-dims (v2 10 10))

; (def level
;   (->
;     (mk-tile-map (:x level-dims) (:y level-dims) :blank)
;     (mix-it-up)))

; (defn make-game-render-data [rd t rendered-level]
;   (assoc rd
;          :xforms
;          (concat
;            (list
;              [:identity]
;              [:clear (cos-01 (* t 1)) 0 (cos-01 (* t 20))]
;              [:scale {:x 10 :y 10}]
;              [:translate (v2/mul (v2/v2 (cos-01 (* t 3)) (cos-01 t)) (v2/v2 20 20))]
;              )
;            (list
;                    [:box {:x 0 :y 0} {:x 20 :y 20} [0 0 0]]
;              )
;            rendered-level)
;          )
;   )

; (defn make-level-render-data [rd t]
;   (assoc rd
;          :xforms (list
;                    [:identity]
;                    [:clear 0 0 1]
;                    [:box {:x 0 :y 0} {:x 20 :y 20} [0 0 0]] )))


; (def rendered-level (vec  (render-level level)))

; (defn update-game [{:keys [tick main-render-data level-render-data] :as game} dt]
;   (let [new-tick (+ dt  tick) ]
;     (do

;       (assoc game
;              :level-render-data (make-level-render-data level-render-data new-tick )
;              :main-render-data (make-game-render-data level-render-data new-tick rendered-level)
;              :tick new-tick))
;     ))

; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; (defn make-pogue-game [renderer resource-manager]
;   (game/make-game
;     (reify
;       game/IGameInit
;       (game-init [_]
;         (println "initialised game!"))

;       game/IGameUpdate
;       (game-update [this dt]
;         (println "updated game!")))))

; (def img-chan (chan))
; (def rt-chan (chan))

; (defn om-loader [_ owner]
;   (reify
;     om/IWillMount
;     (will-mount [_]
;       (go-loop []
;                (let [[msg port] (alts! [img-chan rt-chan])]
;                  (cond
;                    (= port img-chan) (println (str "img: " msg))
;                    (= port rt-chan) (println (str "rt: " msg))
;                    ))
;                (recur)))

;     om/IDidUpdate
;     (did-update [this next-props next-state]
;       )

;     om/IRender
;     (render [_]
;       (dom/p nil "Loaded")
;       )
;     )
;   )

; (defonce loader-atom (atom {:rt-chan (chan) :img-chan (chan)}))

; (defn str->bytes [s]
;   (->
;     (fn [ch]
;       (let [cc (.charCodeAt ch 0)]
;         (if (> cc 255)
;           [(bit-and cc 255) (bit-shift-right cc 8)]
;           [cc])))
;     (mapcat s)))

; (defn col->array [col]
;   (let [arr #js []]
;     (doseq [i (range (count col))]
;       (.push arr (nth col i)))
;     arr))

; (defn str->enc64 [s]
;   (b64/encodeByteArray (-> s str->bytes col->array)))

; (defn str->inline-png [s]
;   (str "data:image/png;base64," (str->enc64 s)))

; (defn <!-inline-img-enc [url]
;   (let [ret (chan)]
;     (go (->>
;           (http/get url)
;           (<!)
;           (:body)
;           (put! ret)))
;     ret))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;





; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; (defonce first-time? (atom true))

; (defn main' []
;   (om/root
;     (fn [game-state owner]
;       (reify
;         om/IWillMount
;         (will-mount [_ ]
;           (go-loop []
;                    (let [dt (<! update-chan) ]
;                      (om/transact! game-state #(update-game % dt))
;                      (recur))))

;         om/IRender
;         (render [_]
;           (dom/div #js {:id "wrapper"}
;                    (dom/div nil (dom/h1 nil (-> game-state :main-app :name)))
;                    (dom/p nil (-> game-state :tick))
;                    ))))
;     app-state
;     {:target (. js/document (getElementById "app"))})

;   (when @first-time?
;     (do
;       (swap! first-time? not)
;       (js/setInterval (fn [] (put! update-chan (/ 1 60))) 16))))


