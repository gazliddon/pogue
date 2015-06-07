(ns gaz.gamerender
  (:require
    [gaz.vec3 :as v3]
    [gaz.vec2 :as v2]
    [gaz.renderprotocols :as rp]))

(defrecord RenderData [dims scale bg-col boxes])

(def game-render-data 
  (RenderData.  (v2/v2 1600 900) 0.5 [0 0 0] []))

(defn get-scaled-dims [^RenderData {:keys [dims scale]}]
  (v2/mul dims (v2/v2 scale scale)))

(defn start [^RenderData rd] rd)
(defn finish [^RenderData rd] (assoc rd [:boxes] []))

(defn render! [^RenderData rd ^rp/IRenderBackend renderer]
  (let [{:keys [bg-col boxes]} rd]
    (rp/clear! renderer bg-col)
    (doseq [{:keys [pos dims col]} boxes]
      (rp/box! renderer [pos dims col]))))

