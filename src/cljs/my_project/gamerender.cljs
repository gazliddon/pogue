(ns gaz.gamerender
  (:require
    [gaz.vec2 :as v2]
    [gaz.renderprotocols :as rp]))

(defrecord RenderData [dims scale bg-col boxes])

(def game-render-data 
  (RenderData.
    [1600 900]
    0.5
    [0 0 1]
    []))

(defn get-scaled-dims [^RenderData {:keys [dims scale]}]
  (let [[w h] dims]
    [ (* scale w) (* scale h)]))

(defn start [^RenderData rd]
  rd)

(defn finish [^RenderData rd]
  (assoc rd [:boxes] []))

(defn draw-box [^RenderData {:keys [boxes] :as rd} x y w h col]
  (assoc rd [:boxes] (conj {:x x :y y :w w :h h :col col} boxes)))

(defn render! [^RenderData rd ^rp/IRenderBackend renderer]
  (let [{:keys [bg-col boxes]} rd]
    (rp/clear! renderer bg-col)
    (doseq [{:keys [x y w h col]} boxes]
      (rp/box! renderer [ x y w h ] col))))

