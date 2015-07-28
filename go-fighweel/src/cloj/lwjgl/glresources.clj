(ns 
  ^{:author "gazl"
    :doc    "gl resource managing. yuck mutable state"}
  
  cloj.lwjgl.glresources)

(defonce current-session (atom 0))

(defn new-session []
  (swap! current-session inc))

(defn get-session []
  @current-session)

(defn mk-gl-resource [func]
  (let [result (atom nil)
        session-id (atom (get-session)) ]
    (reify
      clojure.lang.IDeref 
      (deref [_]
        (let [current-session (get-session)]
          (when (or (nil? @result) (not= session-id (get-session)))
            (reset! result (func))
            (reset! session-id (get-session))))
        @result))))

(defmacro delay-ish [forms]
  (mk-gl-resource
    `(fn []
       ~@forms)))
