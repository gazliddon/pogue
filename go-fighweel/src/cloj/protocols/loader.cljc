(ns cloj.protocols.loader)

(defprotocol ILoader
  (load-blocking! [_ file-name])
  (load-async! [_ file-name])
  )
