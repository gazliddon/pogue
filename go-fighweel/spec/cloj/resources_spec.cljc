(ns cloj.resources-spec
  (:require [speclj.core :refer :all]

            [cloj.lwjgl.system :as sys]

            [cloj.protocols.resources :as res-p]
            [cloj.protocols.window    :as win-p]
            [cloj.protocols.system    :as sys-p]
            [cloj.protocols.render    :as rend-p]

            [cloj.math.vec2 :refer [v2f]]

            [clojure.core.async :as async :refer [timeout >! <!! <! go alts!!]]))

(def test-file-name   "test-data/blocks.png")
(def my-sys (sys/mk-system))
(def my-window (sys-p/get-window my-sys))
(def res-manager (sys-p/get-resource-manager my-sys))

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


(def the-img (atom nil))

(describe "async image loading tests"

  (before-all
    (win-p/create! my-window (v2f 100 100) "test window"))

  (after-all
    (win-p/destroy! my-window))

  (with-all
    the-img (->>
              test-file-name
              (res-p/load-img! res-manager :poo)
              (async-op-as-sync)))

  (it "should satisfie the IImage protocol"
    (should (satisfies? rend-p/IImage @the-img)))

  (it "should be the right width"
    (should= 320 (rend-p/width @the-img)))

  (it "should be the right height"
    (should= 240 (rend-p/height @the-img)))

  ; (it "should have an opengl texture id"
  ;   (should (pos? (:tex-id (rend-p/img @the-img)))))

  ; (it "should have have these keys"
  ;   (let [mp (rend-p/img @the-img)]
  ;     (should (every? #(contains? mp %) [:tex-id :width :height])))
  ;   )
  )


(run-specs)
