(ns cloj.web.utils
  (:require 
    [cljs-http.client       :as http]
    [hipo.core              :as hipo  :include-macros true]
    [dommy.core             :as dommy :include-macros true]  ))

(defn id-ize   [v] (str "#" v))
(defn by-id    [v] (-> (id-ize v) (dommy/sel1)))
(defn get-dims [e] (mapv e [:width :height]))
(defn log-js   [v] (.log js/console v))
