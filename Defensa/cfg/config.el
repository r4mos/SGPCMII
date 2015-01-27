(TeX-add-style-hook "config"
 (lambda ()
    (LaTeX-add-environments
     "slide"
     "blankslide")
    (TeX-add-symbols
     '("icon" 1)
     "frametit"
     "framesubtit")))

