(ns cljam.util.t-chromosome
  (:require [clojure.test :refer :all]
            [cljam.util.chromosome :as chr]))

(deftest normalize-chromosome-key
  (are [?key ?normalized-key] (= (chr/normalize-chromosome-key ?key)
                                 ?normalized-key)
    "chr1" "chr1"
    "chr1" "chr1"
    "chr01" "chr1"
    "chr22" "chr22"
    "chrX" "chrX"
    "chrY" "chrY"
    "chrM" "chrM"
    "chrMT" "chrMT"

    "Chr1" "chr1"
    "Chr01" "chr1"
    "Chr22" "chr22"
    "ChrX" "chrX"
    "ChrY" "chrY"
    "ChrM" "chrM"
    "ChrMT" "chrMT"

    "CHR1" "chr1"
    "CHR01" "chr1"
    "CHR22" "chr22"
    "CHRX" "chrX"
    "CHRY" "chrY"
    "CHRM" "chrM"
    "CHRMT" "chrMT"

    "1" "chr1"
    "01" "chr1"
    "22" "chr22"
    "X" "chrX"
    "Y" "chrY"
    "M" "chrM"
    "MT" "chrMT"

    "x" "chrX"
    "y" "chrY"
    "m" "chrM"
    "mt" "chrMT"

    "_1" "_1"
    "GL000226_1" "GL000226_1"
    "GL000207.1" "GL000207_1"
    "NC_007605" "NC_007605"
    "hs37d5" "hs37d5"

    ;; TODO: Add more SN name
    )

  (are [?key ?trimmed-key] (= (chr/trim-chromosome-key ?key) ?trimmed-key)
    "chr1" "1"
    "Chr2" "2"
    "CHR3" "3"
    "4" "4"
    "X" "X"

    ;; TODO: Add more SN name
    ))
