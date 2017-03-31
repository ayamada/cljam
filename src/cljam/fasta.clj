(ns cljam.fasta
  "Alpha - subject to change.
  Reader of a FASTA format file."
  (:refer-clojure :exclude [read])
  (:require [cljam.fasta.core :as fa-core]
            [cljam.fasta.reader])
  (:import cljam.fasta.reader.FASTAReader))

(defn ^FASTAReader reader
  "Returns FASTA file reader of f."
  [f]
  (fa-core/reader f))

(defn read
  [rdr]
  (fa-core/read rdr))

(defn read-headers
  "Returns headers as vector. Each element consists of name and offset."
  [rdr]
  (fa-core/read-headers rdr))

(defn read-indices
  ""
  [rdr]
  (fa-core/read-indices rdr))

(defn read-sequences
  "Reads sequences by line, returning the line-separated sequences
  as lazy sequence."
  [rdr]
  (fa-core/read-sequences rdr))

(defn read-sequence
  [rdr {:keys [chr start end]}]
  (cond
    (and chr start end) (fa-core/read-sequence rdr chr start end)
    chr (fa-core/read-sequence rdr chr)
    :else (fa-core/read-sequences rdr)))

(defn reset
  [rdr]
  (fa-core/reset rdr))

(defn sequential-read
  "Reads entire sequences sequentially on caller's thread,
   blocking until entire file is loaded.
   Supporting raw (.fa) and compressed FASTA (.fa.gz, .fa.bz2, etc.)."
  [^String f]
  (fa-core/sequential-read f))
