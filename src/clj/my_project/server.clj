(ns my-project.server
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [my-project.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [base64-clj.core :as base64]
            [clojure.java.io :refer [file output-stream input-stream]]
            [ring.adapter.jetty :refer [run-jetty]]))

(deftemplate page (io/resource "index.html") []
  [:body] (if is-dev? inject-devmode-html identity))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn read-file [file-name]
  (let [f (file file-name)
        length (.length f)
        buffer (byte-array length)]
    (with-open[in (input-stream (file file-name))]
      (.read in buffer 0 length))
    buffer
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})


  (GET "/" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (wrap-defaults #'routes api-defaults))
    (wrap-defaults routes api-defaults)))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (print "Starting web server on port" port ".\n")
    (run-jetty http-handler {:port port :join? false})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel))

(defn run [& [port]]
  (when is-dev?
    (run-auto-reload))
  (run-web-server port))

(defn -main [& [port]]
  (run port))


(defn rand-v2 []
  {:x (rand-int 100)
   :y (rand-int 100)})


(defn bits->bools [n bits]
  (loop [c 0
         r '()]
    (if (= c bits)
      r
      (recur (inc c)
             (cons (bit-test n c) r))
      )))

(defn bmap [v t f]
  (->>
    (mapv #(if % t f) (bits->bools v 4))
    (partition 2)
    (mapv vec)))



