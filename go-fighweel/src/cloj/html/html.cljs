(ns cloj.html
  (:require
    [cloj.html.keyboard]
    [cloj.html.loader]
    [cloj.html.logger]
    [cloj.html.render]
    [cloj.html.resource]
    [cloj.html.timer] 
    [cloj.html.system]
    [cloj.html.utils]
    )
  (:require-macros [cloj.macros :refer [import-vars]]))

(import-vars 'cloj.html.keyboard)
(import-vars 'cloj.html.loader)
(import-vars 'cloj.html.logger)
(import-vars 'cloj.html.render)
(import-vars 'cloj.html.resource)
(import-vars 'cloj.html.timer) 
(import-vars 'cloj.html.system)
(import-vars 'cloj.html.utils)

