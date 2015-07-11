(ns cloj.protocols.resources)

(defprotocol IResourceStore)

(defprotocol IResourceManagerInfo
  (find-img [_ id])
  (find-render-target [_ id])
  (list-render-targets [_])
  (list-imgs [_]))

(defprotocol IResourceManager
  (clear-resources! [_])
  (create-render-target! [_ id w h])
  (load-img! [_ id source]))
