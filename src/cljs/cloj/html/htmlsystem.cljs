(ns cloj.html.system
  (:require
    [cloj.system         :as system]
    [cloj.keyboard       :as keyboard]

    [cloj.html.logger    :as html-logger]
    [cloj.html.keyboard  :as html-keyboard]
    [cloj.html.loader    :as html-loader]
    [cloj.html.timer     :as html-timer]
    [cloj.html.resource  :as html-resource]
    [cloj.html.render    :as html-render]
    )
  )

(defn mk-html-system []
  (let [logger             nil
        keyboard           nil
        loader             nil
        timer              nil
        resource-mananager nil
        render-engine      nil]

    (system/System.
      logger
      keyboard
      loader
      timer
      resource-mananager
      render-engine)))

(extend-type system/System
  system/ISystem!
  (init! [ {:keys [keyboard]} ]
    (keyboard/init! keyboard))

  (update! [ {:keys [keyboard] }]
    (keyboard/update! keyboard)
    )
  )


