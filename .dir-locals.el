((nil . ((eval . (progn
                   (defun figwheel-repl ()
                     (interactive)
                     (run-clojure "lein figwheel ios"))
                   
                   (add-hook 'clojure-mode-hook #'inf-clojure-minor-mode))))))


