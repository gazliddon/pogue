(ns glom.core
  )

; Would work well to copy om but there's no declarative order
; for rendering stuff liek in the dom
; but I could my own small noddy layout language?

; Called only once on an Om component. Implementations should return a map of initial state
(defprotocol IInitState
  (init-state [this]))

; render component, stateless
(defprotocol IRender
  (render [this]
          (with-gl this  ;; could this encode context?
            (glBindVertexArray vao-id)
            (glDrawElements GL_TRIANGLE_STRIP num-of-indicies GL_UNSIGNED_INT 0))))

; before mounting into ogl
; mounting would be resource acquisition
(defprotocol IWillMount
  (will-mount [this]))

; has mounted into ogl
(defprotocol IDidMount
  (did-mount [this]))

(defprotocol IRenderState
  (render-state [this state]))

(defn draw-model [file-name]
  (fn [app owner]
    (reify
      IInitState
      (init-state [_]
        (with-gl this
          (glBindVertexArray vao-id)))

      IRenderState
      (render [this {:keys [vao-id num-of-indicies]}]
        (with-gl this  ;; could this encode context?
          (glDrawElements GL_TRIANGLE_STRIP num-of-indicies GL_UNSIGNED_INT 0)))  
      ))

  )

