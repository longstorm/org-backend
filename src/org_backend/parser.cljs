(ns org-backend.parser
  (:require [org-backend.core :refer [slurp]]))

(defn basename [path ext]
  (let [p (clojure.string/replace path ext "")
        idx (.lastIndexOf path "/")]
    (if (= -1 idx) p (subs p (inc idx)))))

(defn org-delimiter [level]
  (str "(\\n|^)\\s*"
       (clojure.string/join (repeat (inc level) "\\*"))
       "[^*]"))

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
  (->> (org-delimiter level)
       re-pattern
       (clojure.string/split s)
       (apply filter-out-empty-chunks)))

(defn parse-chunk [s]
  (let [[headline body] (clojure.string/split s #"\n" 2)]
    (map str [headline body])))

(defn parse-properties [prop-lines]
  (reduce (fn [m line]
            (let [[_ p val] (clojure.string/split line #":")]
              (assoc m (keyword p) (clojure.string/trim val))))
          {} (butlast (rest prop-lines))))

(defn get-leafs-and-properties [intro]
  (let [lines (get-lines intro)]
    (if-not (re-find #"^\s*:(properties|PROPERTIES):" (first lines))
      [{} lines]
      (update-in (split-at
                  (->> lines
                       (take-while #(not (re-find #"^\s*:(end|END):" %)))
                       count inc inc)
                  lines)
                 [0] parse-properties))))

(defn org-outline-shallow [level parsed-chunks]
  (map #(let [[props leafs] (get-leafs-and-properties (second %))]
          {:headline (first %)
           :properties props
           :level (inc level)
           :leafs leafs})
       parsed-chunks))

(defn org-outline
  ([path] (org-outline 0 false (org-file-name path) (slurp path)))
  ([path shallow] (org-outline 0 true (org-file-name path) (slurp path)))
  ([level title s] (org-outline level false title s))
  ([level shallow title s]
     (let [[intro & chunks] (org-outline-chunks level s)
           subtrees (map parse-chunk chunks)
           [props leafs] (get-leafs-and-properties intro)]
       {:headline title
        :properties props
        :level level
        :leafs leafs
        :subtrees (if shallow
                    (org-outline-shallow level subtrees)
                    (map (partial apply
                                  (partial org-outline (inc level) false))
                         subtrees))})))

(defn org-outline-empty
  ([] (org-outline-empty ""))
  ([headline] {:headline headline, :level 0, :leafs [], :subtrees []}))

(defn serialize-outline
  ([node] (serialize-outline node false))
  ([{:keys [headline properties level leafs subtrees]} only-subtrees?]
     (let [frontmatter
           (when-not only-subtrees?
             (concat [(apply str (concat (repeat level "*")
                                         [" " headline]))]
                     (when (seq properties)
                       [(str (reduce-kv (fn [s k v] (str s "  " k ": " v "\n"))
                                         "  :PROPERTIES:\n" properties)
                              "  :END:")])
                     leafs))]
       (clojure.string/join "\n" (concat
                                  frontmatter
                                  (when (seq subtrees)
                                    (map serialize-outline subtrees)))))))
