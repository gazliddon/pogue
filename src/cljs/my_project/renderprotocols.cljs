(ns gaz.renderprotocols
  )

(defprotocol IRenderBackend
  (clear! [this col])
  (box! [this pos col])
  (spr! [this spr-id pos])
  (spr-scaled! [this spr-id pos dims])
  (load-sprs! [this spr-data]))
