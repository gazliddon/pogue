(ns gaz.gamerender
  (:require
    [gaz.vec3 :as v3]
    [gaz.vec2 :as v2]
    [gaz.renderprotocols :as rp]  
    [gaz.renderutils :as ru]))

(defrecord RenderData [dims scale bg-col boxes xforms])

(def main-render-data 
  (RenderData.
    (v2/v2 1600 900)
    0.5
    [0 0 0]
    []
    []))

(def level-render-data 
  (RenderData.
    (v2/v2 1600 900)
    0.25
    [0 1 0]
    []
    []))

(defn get-scaled-dims [^RenderData {:keys [dims scale]}]
  (v2/mul dims (v2/v2 scale scale)))

(defn start [^RenderData rd] rd)
(defn finish [^RenderData rd] (assoc rd [:boxes] []))

(defn render! [^RenderData {:keys [bg-col boxes xforms] :as rd} renderer]
  (rp/identity! renderer)
  (rp/clear! renderer bg-col)
  (ru/do-xforms! renderer xforms)
  (doseq [{:keys [pos dims col]} boxes]
    (rp/box! renderer [pos dims col])))

