(ns hundred-pushups.node-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [hundred-pushups.core-test]
            ))

(doo-tests 'hundred-pushups.core-test
           )
