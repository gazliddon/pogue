(ns gaz.gfxcomp
  (:require
    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

(defn make-gfx-component [ files ref-id ]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/div
          #js {:ref ref-id}
          (dom/img #js {:src "data/tiles.png"
                        :ref (str ref-id "-tiles")}))))))
