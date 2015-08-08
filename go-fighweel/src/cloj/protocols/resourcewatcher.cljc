(ns cloj.protocols.resourcewatcher) 

(defprotocol IResourceWatcher
  (stop! [_])
  (watch! [_ sub-resource ch])
  (on-change! [_ file]))
