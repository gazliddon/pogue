(ns cloj.loader-spec
  (:require [speclj.core :refer :all]
            [clojure.core.async :as async :refer [timeout chan >! <!! <! go alts!!]]
            [cloj.jvm.loader :as loader]
            [cloj.protocols.loader :as loader-p]))


(def test-file-name   "test-data/blocks.png")
(def test-file-digest "284fe63d77a4398957a30a658a0f439a7ca20395e4b63a01122b37c7ab74eed2")


(defmacro <? [ch]
  `(throw-err (async/<! ~ch)))

(defn throw-err [e]
  (when (instance? Throwable e) (throw e))
  e)

(defn- async-op-as-sync [ in-chan ]
  (let [timeout-chan (timeout 1000)
        [r p] (alts!! [timeout-chan in-chan])
        res (throw-err r)
        ]
    (if (nil? res)
      "Timed out"
      r)))

(def my-loader (loader/mk-loader))

(defn load-blocking! [file-name]
  (loader-p/load-blocking! my-loader file-name))

(defn load-async! [file-name]
  (async-op-as-sync (loader-p/load-async! my-loader file-name)))

(describe "Blocking load tests"
  (with-all the-file (load-blocking! test-file-name))

  (it "should return a byte buffer"
    (should= (Class/forName "[B") (type (:data @the-file))))

  (it "should be the same size as the size field"
    (should= (:size @the-file) (count (:data @the-file))))

  (it "should have the right file name"
    (should= test-file-name (:file-name @the-file)))

  (it "should have the right file size"
    (should= 9705 (:size @the-file)))

  (it "should match the digest"
    (should= test-file-digest (:digest @the-file))))

(describe "Async loading tests"
  (with-all the-file (load-async! test-file-name))

  (it "should return a byte buffer"
    (should= (Class/forName "[B") (type (:data @the-file))))

  (it "should be the same size as the size field"
    (should= (:size @the-file) (count (:data @the-file))))

  (it "should have the right file name"
    (should= test-file-name (:file-name @the-file)))

  (it "should have the right file size"
    (should= 9705 (:size @the-file)))

  (it "should match the digest"
    (should= test-file-digest (:digest @the-file))))

(run-specs)
