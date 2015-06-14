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
    (println file-name)
    (with-open[in (input-stream (file file-name))]
      (.read in buffer 0 length))
    buffer
    ))


(defn get-enc-64-as-str [file]
  (->>
    file
    (read-file)
    (base64/encode-bytes)
    (map char)
    (apply str)))

(defn get-file-as-mime-type [rez-name mime-type]
  (->>
    (io/resource rez-name)
    (get-enc-64-as-str)
    (str "data:" mime-type ";base64,")))

(defn get-png-enc [name]
  (get-file-as-mime-type (str  "public/data/" name ".png"), "image/png"))

(def type-to-mtype
  {:png "image/png"})

(defn get-rez [rez-name rez-type]
  (let [file-name (str "public/data/" rez-name "." rez-type)
        mime-type ((keyword rez-type) type-to-mtype) ]
    (get-file-as-mime-type file-name mime-type)
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})

  (GET ["/rez/:rez-type/:rez-name" :rez-name #".*" :rez-type #".*"] [rez-name rez-type]
       ; (str rez-name " " rez-type)
       (get-rez rez-name rez-type)
       )

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






