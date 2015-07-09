(ns cloj.resources-spec
  (:require [speclj.core :refer :all]
            [cloj.core :refer :all]
            [digest :as digest]
            [cloj.keyboard :as kb]
            [cloj.lwjgl.resources :as ac]
            [cloj.resources.manager :as rman]
            [cloj.render.protocols :as rp]
            [cloj.lwjgl.system :as sys]
            [clojure.core.async :as async :refer [timeout chan >! <!! <! go alts!!]]))

(def the-window (sys/mk-lwjgl-window))
(defn open-window [] (sys/create the-window 200 200 "testt"))
(defn close-window[] (sys/destroy the-window))
(def test-file-name   "test-data/blocks.png")
(def test-file-digest "284fe63d77a4398957a30a658a0f439a7ca20395e4b63a01122b37c7ab74eed2")

(def resman (ac/mk-resource-manager))

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

(defn- load-async-sync [file-name]
  (async-op-as-sync (ac/load-async file-name)))

(describe "async image loading tests"
          (with-all
            the-img ( ->>
                      test-file-name
                      (rman/load-img! resman :poo)
                      (async-op-as-sync)))
          
          (before-all (open-window))
          (after-all (close-window))

          (it "should satisfie the IImage protocol"
              (should (satisfies? rp/IImage @the-img)))

          (it "should be the right width"
              (should= 320 (rp/width @the-img)))

          (it "should be the right height"
              (should= 240 (rp/height @the-img)))

          (it "should have an opengl texture id"
              (should (pos? (:tex-id (rp/img @the-img)))))

          (it "should have have these keys"
              (let [mp (rp/img @the-img)]
                (should (every? #(contains? mp %) [:tex-id :width :height])))))

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
