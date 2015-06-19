(ns cloj.resources.manager)

(defprotocol IImage
  (id [_])
  (width [_])
  (height [_])
  (img [_]))

(defprotocol IResourceManagerInfo
  (find-img [_ id])
  (find-render-target [_ id])
  (list-render-targets [_])
  (list-imgs [_]))

(defprotocol IResourceManager
  (clear-resources! [_])
  (create-render-target! [_ id w h])
  (attach-renderer [this id])
  (load-img! [_ source]))
