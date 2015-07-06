(ns cloj.core-spec
  (:require [speclj.core :refer :all]
            [cloj.core :refer :all]
            [cloj.math.misc :refer :all]))

(describe "Math Tests"
  (it "is a fracted thing"
    (should (float=  0.6502 (fract (+ 4  0.6502)))))

  (it "is a floored double"
    (should= 4.0 (ceil 4.0)))

  (it "is a floored double"
    (should= 4.0 (ceil 3.21212)))

  (it "should clamp things"
      (should
        (float= 0 (clamp01 -1)))
      (should
        (float= 1 (clamp01 199)))
      (should
        (float= 0.6502 (clamp01 0.6502)))
      )

  (it "should do this right"
      (should= 4 (num-digits 1000 10))
      )
  (it "should count digits"
      (should=
        [1 2 3 4 5 6]
        (mapv #(num-digits % 10)  [1 10 100 1000 10000 100000])
        
        )
      )
  )

(run-specs)


