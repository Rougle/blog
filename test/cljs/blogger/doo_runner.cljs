(ns blogger.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [blogger.core-test]))

(doo-tests 'blogger.core-test)

