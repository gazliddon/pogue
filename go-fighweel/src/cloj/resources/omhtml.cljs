(ns cloj.resources.omhtml
  #_(:require [cloj.resources.manager :as rman]
            [cloj.render.canvas :refer [canvas-immediate-renderer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]  
            [dommy.core :as dommy :refer-macros [sel sel1]]))

; (defn resource-img [{:keys [id]} owner]
;   (let [src (str "/data/" id ".png")]
;     (reify
;       om/IRenderState
;       (render-state [_ state]
;         (dom/img #js {:id id :src src})))))

; (defn make-canvas-component[{:keys [id dims]} owner]
;   (let [{w :x h :y} dims]
;     (reify
;       om/IDidMount
;       (did-mount [ this ])
;       om/IRenderState
;       (render-state [this state]
;         (dom/canvas #js {:id id :width w :height h})))))

; (defrecord OmResManager [om-res res-atom]
;   rman/IResourceManager
;   (create-render-target! [this id w h]
;     (swap! res-atom
;            update :targets #(cons {:id id :dims {:x w :y h}} % )))

;   (load-img! [this id]
;     (swap! res-atom update :imgs #(cons {:id id} % ))))

; (defn mk-om-res [div-name res-atom]
;   (let [elem (. js/document (getElementById div-name))]
;     (om/root
;       (fn [{:keys [imgs targets]} owner]
;         (reify

;           om/IRender
;           (render [_]
;             (dom/div
;               nil 
;               (apply dom/div nil (om/build-all resource-img imgs))
;               (apply dom/div nil (om/build-all make-canvas-component targets))))))
;       res-atom {:target elem})))


; (defn mk-om-res [div-name res-atom]
;   (let [elem (. js/document (getElementById div-name))]
;     (om/root
;       (fn [{:keys [imgs targets]} owner]
;         (reify

;           om/IRender
;           (render [_]
;             (dom/div
;               nil 
;               (apply dom/div nil (om/build-all resource-img imgs))
;               (apply dom/div nil (om/build-all make-canvas-component targets))))))
;       res-atom {:target elem})))
