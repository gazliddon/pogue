(ns transtest.core
  (:require
    [cognitect.transit :as transit]
    )

  (:import
    [java.io ByteArrayInputStream ByteArrayOutputStream])
  )

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

;; Write data to a stream
(def out (ByteArrayOutputStream. 4096))
(def writer (transit/writer out :json))
(transit/write writer "foo")
(transit/write writer {:a [(float 1) (float 2)]})
(transit/write writer (float 1))

;; Take a peek at the JSON
(prn  (.toString out))
;; => "{\"~#'\":\"foo\"} [\"^ \",\"~:a\",[1,2]]"

;; Read data from a stream
(def in (ByteArrayInputStream. (.toByteArray out)))
(def reader (transit/reader in :json))
(prn (transit/read reader))  ;; => "foo"
(prn (transit/read reader))  ;; => {:a [1 2]}
