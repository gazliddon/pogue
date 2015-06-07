(ns gaz.renderprotocols
  )

(defprotocol IRenderBackend
  (clear! [this info])
  (box! [this info])
  (spr! [this info])
  (spr-scaled! [this info])
  (load-sprs! [this info]))


