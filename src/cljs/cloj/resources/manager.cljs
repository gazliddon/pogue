(ns cloj.resources.manager
  )

(defprotocol IResourceManagerInfo
  (find-img [_ id])
  (find-render-target [_ id])
  (list-render-targets [_])
  (list-imgs [_]))

(defprotocol IResourceManager
  (create-render-target! [_ id w h])
  (load-img! [_ source]))
