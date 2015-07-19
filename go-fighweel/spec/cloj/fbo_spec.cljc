(ns cloj.fbo-spec
  (:require [speclj.core :refer :all]

            [cloj.lwjgl.system :as sys]

            [cloj.protocols.resources :as res-p]
            [cloj.protocols.window    :as win-p]
            [cloj.protocols.system    :as sys-p]
            [cloj.protocols.render    :as rend-p]

            [cloj.lwjgl.protocols :refer [IGLFBO
                                          IGLTexture]]

            [cloj.math.vec2 :refer [v2f]]

            [clojure.core.async :as async :refer [timeout >! <!! <! go alts!!]]))

(def my-sys (sys/mk-system))
(def my-window (sys-p/get-window my-sys))
(def res-manager (sys-p/get-resource-manager my-sys))
(def render-manager (sys-p/get-render-manager my-sys))

(describe "testing fbo creation"
  (before-all
    (do
      (win-p/create! my-window (v2f 100 100) "test window")
      (rend-p/init! render-manager)))

  (after-all
    (win-p/destroy! my-window))

  (with-all
    the-fbo (rend-p/make-render-target! render-manager (v2f 256 512)))

  (it "should be the right width"
    (should= 256 (-> @the-fbo :dims :x)))

  (it "should be the right height"
    (should= 512 (-> @the-fbo :dims :y)))

  (it "should have a tex id > 0"
    (should (> (:tex-id @the-fbo) 0)))

  (it "should have a fbo id > 0"
    (should (> (:fbo-id @the-fbo) 0)))

  (it "shouldn't have a z buffer"
    (should= false (:has-z? @the-fbo) ))

  (it "should support IImage"
    (should (satisfies? rend-p/IImage @the-fbo)))

  (it "should support IGLTexture"
    (should (satisfies? IGLTexture @the-fbo)))

  (it "should support IGLFBO"
    (should (satisfies? IGLFBO @the-fbo)))
  )

(run-specs)
