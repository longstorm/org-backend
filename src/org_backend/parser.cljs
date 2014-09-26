(ns org-backend.parser
  (:require [org-backend.core :refer [slurp]]))

(defn basename [path]
  (let [idx (.lastIndexOf path "/")]
    (if (= -1 idx)
      path
      (subs path (inc idx)))))

(defn org-delimiter [level]
  (clojure.string/join (repeat (inc level) "#")))

(defn org-split-link [s]
  (clojure.string/split (subs s 2 (- (count s) 2)) #"\]\["))

(defn org-link-p [s]
  (let [[url title]
        (condp re-find s
          #"^\s*\[\[[^\]]*\]\[[^\]]*\]\]\s*$" (org-split-link s)
          #"^https?://\S*$"                   [s s]
          nil)]
    (when url {:title title :url url :source s})))

(defn org-link [s]
  (or (org-link-p s) s))

(defn org-file-name [path]
  (basename path ".org"))

(defn filter-out-empty [coll]
  (filter (complement #{"" "\n"}) coll))

(defn get-lines [s]
  (filter-out-empty (clojure.string/split s #"\n")))

(defn filter-out-empty-chunks
  "The first chunk (the intro) is never filtered out since an empty
  intro is semantically meaningful and should be kept, whereas empty
  chunks elsewhere are a calculated parsing error and should be here
  corrected."
  [intro & chunks]
  (into [intro] (filter-out-empty chunks)))

(defn org-outline-chunks [level s]
  (->> (str "(\\n|^)\\s*" (org-delimiter level) "[^#]")
       re-pattern
       (split s)
       (apply filter-out-empty-chunks)))

(defn parse-chunk [s]
  (let [[headline body] (clojure.string/split s #"\n" 2)]
    (map str [headline body])))

(defn org-outline-shallow-nodes [level parsed-chunks]
 (map #({:headline (first %),
         :level (inc level)
         :leafs (get-lines (second %))})
      parsed-chunks))

(defn org-outline-nodes
  ([path] (org-outline-nodes 0 false (org-file-name path) (slurp path)))
  ([path shallow] (org-outline-nodes 0 true (org-file-name path) (slurp path)))
  ([level title s] (org-outline-nodes level false title s))
  ([level shallow title s]
     (let [[intro & chunks] (org-outline-chunks level s)
           subtrees (map parse-chunk chunks)]
       {:headline title
        :level level
        :leafs (get-lines intro)
        :subtrees (if shallow
                    (org-outline-shallow-nodes level subtrees)
                    (map (partial apply
                                  (partial org-outline-nodes (inc level) false))
                     subtrees))})))

(defn org-outline-empty
  ([] (org-outline-empty ""))
  ([headline] {:headline headline, :level 0, :leafs [], :subtrees []}))
