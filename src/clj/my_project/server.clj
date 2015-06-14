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

(defn read-file [file-path]
  (with-open [reader (input-stream file-path)]
    (let [length (.length (clojure.java.io/file file-path))
          buffer (byte-array length)]
      (.read reader buffer 0 length)
      buffer)))

(do
  (defn get-enc-64-as-str [file]
    (->>
      file
      (read-file)
      (base64/encode-bytes)
      (map char)
      (apply str) 
      ))

  (defn get-png-enc [name]
    (->>
      (str "resources/public/data/" name ".png" )
      (get-enc-64-as-str )
      )
    )

  (println (get-png-enc "tiles"))
  )

(format
  "%02x"
  (+ 256 -119)
  ) 


(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET ["/rez/:name.img" :name #".*"] [name]
       (str "File:" name ))
  (GET "/*" req (page)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;






