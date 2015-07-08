(ns cloj.resources-spec
  (:require [speclj.core :refer :all]
            [cloj.core :refer :all]
            [digest :as digest]
            [cloj.keyboard :as kb]
            [cloj.lwjgl.resources :as ac]
            [cloj.lwjgl.system :as sys]
            [clojure-gl.texture :as cgltex]
            [clojure.core.async :as async :refer [timeout chan >! <!! <! go]]
            [cloj.math.misc :refer :all]))


(defn- async-op-as-sync [op & args ]
  (let [loader (apply op (timeout 1000)  args) ]
    (or (<!! loader) "timed out doing async shit")))

(defn- load-async-sync [file-name]
  (async-op-as-sync ac/load-async file-name))

(def test-file-name   "test-data/blocks.png")
(def test-file-digest "284fe63d77a4398957a30a658a0f439a7ca20395e4b63a01122b37c7ab74eed2")


(describe "async image loading tests"
          (with-all the-img (async-op-as-sync ac/load-async-img test-file-name))

          (it "should be the right width"
              (should= 320 (.getWidth @the-img)))

          (it "should be the right height"
              (should= 240 (.getHeight @the-img))))

(describe "Blocking load tests"
          (with-all the-file (ac/load-blocking test-file-name))

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
          (with-all the-file (load-async-sync test-file-name))

          (it "should return a byte buffer"
              (should= (Class/forName "[B") (type (:data @the-file))))

          (it "should be the same size as the size field"
              (should= (:size @the-file) (count (:data @the-file))))

          (it "should have the right file name"
              (should= test-file-name (:file-name @the-file)))

          (it "should have the right file size"
              (should= 9705 (:size @the-file)))

          (it "should match the digest"
              (should= test-file-digest (:digest @the-file)))
          )
