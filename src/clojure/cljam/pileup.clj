(ns cljam.pileup
  (:require (cljam [cigar :as cgr]
                   [bam :as bam])))

(def ^:private window-width 1250) ;; TODO: estiamte from actual data
(def ^:private step 2000) ;; TODO: estiamte from actual data
(def ^:private center (int (/ step 2))) ;; TODO: estiamte from actual data

(defn- count-for-alignment
  [^clojure.lang.PersistentHashMap aln
   ^String rname
   ^clojure.lang.LazySeq positions]
  (if (= rname (:rname aln))
    (let [left (:pos aln)
          right (+ (:pos aln) (cgr/count-ref (:cigar aln)))]
      (map (fn [p] (if (and (>= p left)
                            (<= p right)) 1 0)) positions))
    (take (count positions) (repeat 0))))

(defn- count-for-positions
  "Returns a histogram value of the specified position."
  [^clojure.lang.LazySeq alns
   ^String rname positions]
  (if (pos? (count alns))
    (apply map + (map #(count-for-alignment % rname positions) alns))
    (take (count positions) (repeat 0))))

(defn rpositions
  ([^Long start ^Long end]
     (rpositions start end start))
  ([^Long start ^Long end ^Long n]
     (if (>= end n)
       (cons n
             (lazy-seq (rpositions start end (inc n))))
       nil)))

(defn- read-alignments
  [rdr ^String rname ^Long rlength ^Long pos]
  (let [^Long left (let [^Long val (- pos window-width)]
                     (if (< val 0)
                       0
                       val))
        ^Long right (let [^Long val (+ pos window-width)]
                      (if (< rlength val)
                        rlength
                        val))]
    (bam/read-alignments rdr rname left right)))

(defn- search-ref
  [refs rname]
  (first
   (filter (fn [r] (= (:name r) rname))
           refs)))

(defn- pileup*
  ([rdr ^String rname ^Long rlength ^Long start ^Long end]
     (flatten
      (let [parts (partition-all step (rpositions start end))]
        (map (fn [positions]
               (let [^Long pos (if (= (count positions) step)
                                 (nth positions center)
                                 (nth positions (quot (count positions) 2)))
                     ^clojure.lang.LazySeq alns (read-alignments rdr rname rlength pos)]
                 (count-for-positions alns rname positions)))
             parts)))))

(defn pileup
  ([rdr]
     ;; TODO
     )
  ([rdr ^String rname]
     (pileup rdr rname -1 -1))
  ([rdr ^String rname ^Long start* ^Long end*]
     (let [r (search-ref (.refs rdr) rname)]
       (if (nil? r)
         nil
         (pileup* rdr
                  rname (:len r)
                  (if (neg? start*) 0 start*)
                  (if (neg? end*) (:len r) end*))))))
