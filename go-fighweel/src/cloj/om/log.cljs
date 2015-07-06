(ns cloj.om.log
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ])
  (:require
    [cljs.core.async        :refer [put! >! chan <! alts! close!]]
    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))

(defn log-window 
  [{:keys [in-chan class-name] :as data} owner ]
  (let [in-chan    (or in-chan (chan)) 
        class-name (or class-name "pane")]
    (reify
      om/IWillMount
      (will-mount [ this ]
        (go
          (om/set-state! owner :text "")
          (loop []
            (let [txt (<! in-chan)
                  txt-req {:text txt}]
              (om/update-state! owner [:text] #(str % "\n" txt ))
              (recur)))))

      om/IRenderState
      (render-state [_ {:keys [text]}]
        (dom/div
          #js { :className class-name }
          (dom/span nil "Logs")
          (dom/textarea #js {:width "100%" :value text}))))))
