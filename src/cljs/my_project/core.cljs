;; {{{ Requires
(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm])
  (:require
    [clojure.string         :refer [split join]]

    [cljs-http.client       :as http]


    [cloj.resources.manager :as rman
                            :refer [create-render-target!
                                    load-img!
                                    clear-resources!]]

    [cloj.resources.html    :as rmhtml]

    [cloj.math.misc         :refer [cos-01 log-base-n ceil floor num-digits]]
    [cloj.math.vec2         :as v2 :refer [v2]]
    [cloj.math.vec3         :as v3 :refer [vec3]]

    [cloj.system            :refer [get-resource-manager
                                    get-render-engine]]

    [cloj.utils             :refer [format]] 
    (cloj.render.protocols  :as rp)

    [cloj.web.utils         :refer [by-id log-js]]

    [game.html              :refer [mk-system]]
    [game.game              :as game]

    [gaz.tiles              :refer [mk-tile-map mix-it-up
                                    render-level]]

    [gaz.appstate           :refer [app-state]]
    [gaz.canvascomp         :refer [build-canvas-component ]]


    [cljs.core.async        :refer [put! >! chan <! alts! close! dropping-buffer mult tap]]
    [ff-om-draggable.core   :refer [draggable-item]]

    [goog.crypt             :as crypt]
    [goog.crypt.base64      :as b64]
    [goog.labs.net.xhr      :as xhr2]

    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;;; }}}

;; =============================================================================
;; {{{ XHR2 experiment

(def file-name "/data/tiles.png")



(defprotocol IBytesPlz
  (get-str [_])
  (get-b64 [_])
  (get-bytes [_ ]))
; var utf8 = unescape(encodeURIComponent(str));

; var arr = [];
; for  var i = 0; i < utf8.length; i++) {
;     arr.push(utf8.charCodeAt(i));

(defn get-bytes-str [this]
  (let [ret (js/Uint8ClampedArray. (count this))]
    (doseq [i (range (count this))]
      (let [c (.charCodeAt this i)]

        (aset ret i (bit-and 0xff(if (< c 0)
                      (- 65536 c)
                      c)) )))
    ret
    ))

(extend-type js/XMLHttpRequest
  IBytesPlz

  (get-str [this]
    (.-responseText this))

  (get-b64 [this]
    (->
      this
      (get-bytes)
      (b64/encodeByteArray)))

  (get-bytes [this]
    (->
      this

      ; (get-str)
      ; (get-bytes-str)
      (get-str)
      (crypt/stringToUtf8ByteArray)
      )))



(def opts #js {"Content-type" "text/plain;charset=x-user-defined"
               "responseType" "blob"})

; (def ret (xhr2/send "GET" file-name nil opts))


; (.then ret (fn [r]
;              (let [arr (get-bytes r)
;                    enc (get-b64 r) 
;                    ostr (get-str r) ]

;                (println (.charCodeAt ostr 0))
;                (println (.charCodeAt ostr 1))
;                (log-js (get-bytes-str ostr))

;                )

;              ))


(defn rep-array [arr]
  (str "size: " (.-length arr) "\n"
       "type: " (type arr) "\n"))

(defn gaz-send [uri cb]
  (let [xhr (js/XMLHttpRequest.) ]
    (do
      (.open xhr "GET" uri true)
      (.overrideMimeType xhr "text/plain; charset=x-user-defined")
      (aset xhr "onreadystatechange"
            (fn [e]
              (let [ready-state (.-readyState xhr)
                    status (.-status xhr)
                    arr (get-bytes-str (.-responseText xhr)) ]
                (when (and (== ready-state 4) (== status 200)) 
                  (cb arr)
                  )
                )))
      (.send xhr))))


(gaz-send file-name (fn [i]
                      (log-js i)
                      ))








;; }}}

;; {{{ Ignore for now
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Game obect
(def pogue-game
  (game/make-game
    (reify 
      game/IGameInit
      (game-init [this]
        this)

      game/IGameUpdate
      (game-update [this dt]
        this)

      game/IGameClose
      (game-close [this]
        this))))
;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ FPS Component

(defn fps-calc-chan
  ([in-chan window-size]
   (let [ret-chan (chan)]
     (go-loop [last-x () fr-num 0]
              (let [dt (<! in-chan)
                    new-last-x (take window-size (cons dt last-x))
                    data  {:avg-fps (/ 1000  (/ (reduce + 0 new-last-x) (count new-last-x)))
                           :min-fps (/ 1000  (apply min new-last-x))
                           :max-fps (/ 1000  (apply max new-last-x))
                           :frame fr-num
                           }
                    
                    ]
                (put! ret-chan data)
                (recur new-last-x (inc fr-num))))
     ret-chan))

  ([in-chan]
   (fps-calc-chan in-chan 10)))

(defn frame-rate-component
  [{:keys [in-chan class-name] :as data} owner ]
  (reify
    om/IWillMount
    (will-mount [ this ]
      (let [fps-chan (fps-calc-chan in-chan) ]
        (go-loop []
                 (om/set-state! owner :fps (<! fps-chan))
                 (recur))))

    om/IRenderState 
    (render-state [_ {:keys [fps]}]
      (when fps 
        (let [elems (map
                     (fn [[txt id]] (dom/p nil (format "%s: %02f" txt (id fps))))
                     [["avg" :avg-fps]
                      ["min" :min-fps]
                      ["max" :max-fps]])]
        (apply dom/div nil  elems))))))
;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Timer
(defprotocol ITimer
  (tick! [_]))

(defn is-valid? [{:keys [previous now] :as c}]
  (not (or (nil? previous) (nil? now))))

(defn time-passed [{:keys [previous now] :as c}]
  (if (is-valid? c)
    (- now previous)
    0))

(defn html-timer []
  (let [c (atom {:previous nil :now nil}) ]
    (reify ITimer
      (tick! [_]
        (do
          (->>
            (assoc @c
                   :previous (:now @c)
                   :now (.now (aget js/window "performance")))
            (reset! c)
            (time-passed)))))))

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Animator with Timer
(def my-timer (html-timer))

(defn animate [callback-fn]
  (do
    (.requestAnimationFrame js/window #(animate callback-fn))
    (let [dt (tick! my-timer)]
      (when (not= dt 0)
        (callback-fn dt)))))

;;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Web Audio
(defprotocol IAudio
  (sq [_ ])
  (tri [_ ])
  (saw [_ ]))

(defprotocol ISFX
  (instrument [_])
  (type! [_ t])
  (freq! [_ v])
  (start! [_])
  (stop! [_])
  (vol! [_ v]))

(defn mk-ins
  ([ctx osc-type]
   (let [ins (mk-ins ctx)]
     (do
       (type! ins osc-type))
     ins))

  ([ctx]
   (let [vco (.createOscillator ctx)
         vca (.createGain ctx)
         ret (reify
               ISFX
               (instrument [_] {:vco vco :vca vca})
               (start! [_] (.start vco))
               (stop!  [_] (.stop vco))
               (type!  [_ osc-type] (set! (.-type vco) osc-type))
               (vol!   [_ volume] (set! (.-value (.-gain vca)) volume))
               (freq!  [_ freq] (set! (.-value  (.-frequency vco)) freq))) ]
     (do
       (.connect vco vca)
       (.connect vca (.-destination ctx)))
     ret))
  )

(defonce audio-html 
  (let [constructor (or js/window.AudioContext
                        js/window.webkitAudioContext)
        ctx (constructor.) ]
    (reify
      IAudio
      (sq [_]
        (mk-ins ctx "square")))))

(defonce sq-1 (sq audio-html))
(defonce sq-2 (sq audio-html))

#_(do
  (freq! sq-1 30)
  (freq! sq-2 30.13721)
  (vol! sq-1 5)
  (vol! sq-2 5)
  (start! sq-1 )
  (start! sq-2 )
  
  
  
  )

;; }}}

;; =============================================================================
;; Game System Stuff {{{


;; Make and test the system first

(def html-system (mk-system "app" "game"))
(def rend (get-render-engine html-system))
(rp/clear! rend [0 1 0])

(defn game-component [data owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (om/set-state! owner :dt 0)
      (let [in-chan (:in-chan data)]
        (go-loop []
          (let [dt (<! in-chan)]
            (om/set-state! owner :dt dt))
          (recur)
          )))

    om/IRenderState
    (render-state [_ state]
      (dom/p nil (str (:dt state))))))

;; }}}

;; =============================================================================
;; {{{ Main

(def log-chan (chan))
(defonce time-chan (chan (dropping-buffer 10)))
(defonce time-chan-mult (mult time-chan))
(defonce g-time (atom 0))

(defonce first-time? (atom true))

(defn funny-col [tm]
  (->>
    (v3/div
      (vec3 tm tm tm)  
      (vec3 1000 500 100))
    (v3/to-vec)
    (mapv cos-01)))

(when @first-time?
  (do
    (swap! first-time? not)
    (->
      (fn [dt]
        (swap! g-time #(+ dt %))
        (do
          (rp/clear! rend (funny-col @g-time))
          (put! time-chan dt)))
      (animate))))

(defn main []
  (do
    (om/root
      frame-rate-component
      {:in-chan (tap time-chan-mult (chan)) }
      {:target (by-id "test") })

    (om/root
      game-component
      {:in-chan (tap time-chan-mult (chan) )}
      {:target (by-id "test") })

    (let [rm (rmhtml/mk-resource-manager "resources")
          _ (clear-resources! rm)
          img-chan (load-img! rm "tiles")
          rend (create-render-target! rm "shit" 300 300) ]
      (go
        (let [img (<! img-chan)]
          (log-js (rman/width img))
          (log-js (rman/height img))))
      )) 

  ; {{{

  ; (om/root
  ;   draggable-log-window
  ;   {:in-chan log-chan :class-name "pane" :position {:left 100 :top 20}}
  ;   {:target (by-id "app")})

  ; (let [rm (rmhtml/mk-resource-manager "resources")
  ;       _ (clear-resources! rm)
  ;       img-chan (load-img! rm "tiles")
  ;       rend (create-render-target! rm "shit" 300 300) ]
  ;   (go
  ;     (let [img (<! img-chan)]
  ;       (log-js (rman/height img))
  ;       (log-js (rman/width img)))))
  ; }}}
  ) 

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;ends
