;; # TeX 🧮
;; ## With KateX and MathJax
(require '[nextjournal.clerk.viewer :as v])

;; The Einstein-Field Equations are:
(v/tex "G_{\\mu\\nu}\\equiv R_{\\mu\\nu} - {\\textstyle 1 \\over 2}R\\,g_{\\mu\\nu} = {8 \\pi G \\over c^4} T_{\\mu\\nu}")
