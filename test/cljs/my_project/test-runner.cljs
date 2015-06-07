(ns my_project.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [my_project.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'my_project.core-test))
    0
    1))
