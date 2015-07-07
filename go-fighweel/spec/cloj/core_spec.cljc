(ns cloj.core-spec
  (:require [speclj.core :refer :all]
            [cloj.core :refer :all]
            [digest :as digest]
            [cloj.keyboard :as kb]
            [async.core :as ac]
            [clojure.core.async :as async :refer [timeout chan >! <!! <! go]]
            [cloj.math.misc :refer :all]))


(defn- load-async-sync [file-name]
  (let [loader (ac/load-async (timeout 1000) file-name) ]
    (or (<!! loader) "timed out loading")))

(def test-file-name "test-data/blocks.png")
(def test-file-digest "284fe63d77a4398957a30a658a0f439a7ca20395e4b63a01122b37c7ab74eed2")

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

(describe "Math Tests"
          (it "is a fracted thing"
              (should (float=  0.6502 (fract (+ 4  0.6502)))))

          (it "is a floored double"
              (should= 4.0 (ceil 4.0)))

          (it "is a floored double"
              (should= 4.0 (ceil 3.21212)))

          (it "should clamp things"

              (should
                (float= 0.6502 (clamp 0.6502 1000 0.6052)))

              (should
                (float= 0 (clamp01 -1)))

              (should
                (float= 1 (clamp01 199)))

              (should
                (float= 0.6502 (clamp01 0.6502))))

          (it "should do this num digits right"
              (should= 4 (num-digits 1000 10)))

          (it "be able to tell if things are in a range"
              (should (in-range? 0.6502 10 0.6502))
              (should (in-range? 10 0.6502 10))
              (should (in-range? 0.6502 10 10))
              (should (in-range? 0.6502 10 5))
              (should-not (in-range? 0.6502 10 -5))
              (should-not (in-range? 0.6502 10 10.00000001)))

          (it "should be able to compare floating point things for equality"
              (should (float= 10 10.00000000000000000000000001)))

          (it "should be able to compare floating point things for equality"
              (should-not (float= 10 10.001)))
          )


(run-specs)


