(ns cljam.core
  (:refer-clojure :exclude [sort merge slurp spit])
  (:require [clj-sub-command.core :refer [do-sub-command]]
            [clojure.tools.cli :refer [cli]]
            (cljam [sam :as sam]
                   [io :as io]
                   [bam :as bam]
                   [bam-indexer :as bai]
                   [sorter :as sorter]
                   [fasta :as fa]
                   [fasta-indexer :as fai]
                   [dict :as dict]
                   [pileup :as plp])
            [cljam.util.sam-util :refer [stringify-header stringify-alignment]])
  (:gen-class))

(defn reader [f]
  (condp re-find f
    #"\.sam$" (sam/reader f)
    #"\.bam$" (bam/reader f)
    (throw (IllegalArgumentException. "Invalid file type"))))

(defn writer [f]
  (condp re-find f
    #"\.sam$" (sam/writer f)
    #"\.bam$" (bam/writer f)
    (throw (IllegalArgumentException. "Invalid file type"))))

(defmulti read-header type)

(defmethod read-header cljam.sam.reader.SAMReader
  [rdr]
  (io/read-header rdr))

(defmethod read-header cljam.bam.reader.BAMReader
  [rdr]
  (io/read-header rdr))

(defmulti read-alignments type)

(defmethod read-alignments cljam.sam.reader.SAMReader
  [rdr]
  (io/read-alignments rdr {}))

(defmethod read-alignments cljam.bam.reader.BAMReader
  [rdr]
  (io/read-alignments rdr {}))

(defmulti read-refs type)

(defmethod read-refs cljam.sam.reader.SAMReader
  [rdr]
  (io/read-refs rdr))

(defmethod read-refs cljam.bam.reader.BAMReader
  [rdr]
  (io/read-refs rdr))

(defn- slurp
  [f]
  (condp re-find f
    #"\.sam$" (sam/slurp f)
    #"\.bam$" (bam/slurp f)
    (throw (IllegalArgumentException. "Invalid file type"))))

(defn- spit
  [f sam]
  (condp re-find f
    #"\.sam$" (sam/spit f sam)
    #"\.bam$" (bam/spit f sam)
    (throw (IllegalArgumentException. "Invalid file type"))))

(defn view [& args]
  (let [[opt [f _] help] (cli args
                              "Usage: cljam view [--header] [--format <auto|sam|bam>] <in.bam|sam>"
                              ["-h" "--help" "Print help" :default false :flag true]
                              ["--header" "Include header" :default false :flag true]
                              ["-f" "--format" "Input file format <auto|sam|bam>" :default "auto"])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (with-open [r (condp = (:format opt)
                    "auto" (reader     f)
                    "sam"  (sam/reader f)
                    "bam"  (bam/reader f))]
      (when (:header opt)
        (println (stringify-header (read-header r))))
      (doseq [aln (read-alignments r)]
        (println (stringify-alignment aln))))
    nil))

(defn convert [& args]
  (let [[opt [in out _] help] (cli args
                                    "Usage: cljam convert [--input-format <auto|sam|bam>] [--output-format <auto|sam|bam>] <in.bam|sam> <out.bam|sam>"
                                    ["-h" "--help" "Print help" :default false :flag true]
                                    ["-if" "--input-format" "Input file format <auto|sam|bam>" :default "auto"]
                                    ["-of" "--output-format" "Output file format <auto|sam|bam>" :default "auto"])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (let [asam (slurp in)]
      (spit out asam))
    nil))

(defn sort [& args]
  (let [[opt [in out _] help] (cli args
                                   "Usage: cljam sort [--order <coordinate|queryname>] <in.bam|sam> <out.bam|sam>"
                                   ["-h" "--help" "Print help" :default false :flag true]
                                   ["-o" "--order" "Sorting order of alignments <coordinate|queryname>" :default "coordinate"])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (let [r (reader in)
          w (writer out)]
      (condp = (:order opt)
        (name sorter/order-coordinate) (sorter/sort-by-pos r w)
        (name sorter/order-queryname) (sorter/sort-by-qname r w)))
    nil))

(defn index [& args]
  (let [[opt [f _] help] (cli args
                              "Usage: cljam index <in.bam>"
                              ["-h" "--help" "Print help" :default false :flag true])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (bai/create-index f (str f ".bai"))
    nil))

(defn idxstats [& args]
  ;; (with-command-line args
  ;;   "Usage: cljam idxstats <aln.bam>"
  ;;   [files]
  ;;   (when-not (= (count files) 1)
  ;;     (println "Invalid arguments")
  ;;     (System/exit 1))
  ;;   (idxr/bam-index-stats (first files)))
  )

(defn merge [& args]
  ;; (with-command-line args
  ;;   "Usage: cljam merge <in1.bam|sam> <in2.bam|sam> ... <out.bam|sam>"
  ;;   [files]
  ;;   (when (< (count files) 2)
  ;;     (println "Invalid arguments")
  ;;     (System/exit 1)))
  )

(defn pileup [& args]
  (let [[opt [f _] help] (cli args
                              "Usage: cljam pileup <in.bam>"
                              ["-h" "--help" "Print help" :default false :flag true])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (with-open [r (reader f)]
      (when (= (type r) cljam.sam.reader.SAMReader)
        (println "Not support SAM file")
        (System/exit 1))
      (when-not (sorter/sorted? r)
        (println "Not sorted")
        (System/exit 1))
      (doseq [rname (map :name (read-refs r))
              line  (plp/mpileup r rname)]
        (if-not (zero? (:count line))
         (println (clojure.string/join \tab (map val line))))))
    nil))

(defn faidx [& args]
  (let [[opt [f _] help] (cli args
                              "Usage: cljam faidx <ref.fasta>"
                              ["-h" "--help" "Print help" :default false :flag true])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (fai/spit (str f ".fai")
              (fa/slurp f))
    nil))

(defn dict [& args]
  (let [[opt [in out _] help] (cli args
                                   "Usage: cljam dict <ref.fasta> <out.dict>"
                                   ["-h" "--help" "Print help" :default false :flag true])]
    (when (:help opt)
      (println help)
      (System/exit 0))
    (dict/create-dict in out)
    nil))

(defn -main [& args]
  (do-sub-command args
                  "Usage: cljam {view,convert,sort,index,pileup,faidx} ..."
                  [:view     cljam.core/view     "Extract/print all or sub alignments in SAM or BAM format."]
                  [:convert  cljam.core/convert  "Convert SAM to BAM or BAM to SAM."]
                  [:sort     cljam.core/sort     "Sort alignments by leftmost coordinates."]
                  [:index    cljam.core/index    "Index sorted alignment for fast random access."]
                  ;; [:idxstats cljam.core/idxstats "Retrieve  and print stats in the index file."]
                  ;; [:merge    cljam.core/merge    "Merge multiple SAM/BAM."]
                  [:pileup   cljam.core/pileup   "Generate pileup for the BAM file."]
                  [:faidx    cljam.core/faidx    "Index reference sequence in the FASTA format."]
                  [:dict     cljam.core/dict     "Create a FASTA sequence dictionary file."]))
