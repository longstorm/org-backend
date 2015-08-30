(ns org-backend.parser
  (:require [clojure.string :as strng]
            [org-backend.core :refer [slurp]]))

(defn basename
  ([path]
   (let [[p slash] (let [s (.lastIndexOf path "/")
                         last-char (-> path count dec)
                         q (subs path 0 last-char)]
                     (if (and (> (count path) 0) (= s last-char))
                       [q (.lastIndexOf q "/")]
                       [path s]))]
     (if (or (= -1 slash) (= 1 (count path)))
       p
       (subs p (inc slash)))))
  ([path ext]
   (let [[p ext-segment] (map (partial apply str)
                              (split-at (- (count path) (count ext)) path))]
     (when (= ext-segment ext)
       (basename p)))))

(defn org-delimiter [level]
  (str "(\\n|^)\\s*"
       (strng/join (repeat (inc level) "\\*"))
       "[^*]"))

(defn org-split-link [s]
  (strng/split (subs s 2 (- (count s) 2)) #"\]\["))

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

(def org-file? org-file-name)

(defn filter-out-empty-chunks
  "The first chunk (the intro) is never filtered out since an empty
  intro is semantically meaningful and should be kept, whereas empty
  chunks elsewhere should be here corrected."
  [intro & chunks]
  (->> (remove #{"" "\n"} chunks)
       (into [intro])))

(defn parse-subtree-chunks [level s]
  (->> (org-delimiter level)
       re-pattern
       (strng/split s)
       (apply filter-out-empty-chunks)))

(defn parse-chunk [s]
  (let [[headline body] (strng/split s #"\n" 2)]
    (map str [headline body])))

(defn parse-properties [prop-lines]
  (reduce (fn [m line]
            (let [[p val] (strng/split line #":")]
              (assoc m (keyword p) (and (seq val) (strng/trim val)))))
          {} prop-lines))

(def subtree-props-parse-cfg
  {:props-extract #(butlast (rest %))
   :props-start #"^\s*:(properties|PROPERTIES):"
   :props-keep #(not (re-find #"^\s*:(end|END):" %))
   :props-prefix #"^\s*:"
   :props-split-inc inc})

(defn get-leafs-and-properties [intro cfg]
  (let [[p & paras] (remove #{""} (strng/split intro #"\n\n"))]
    (if-not (and (string? p) (re-find (:props-start cfg) p))
      [{} (cond (nil? paras) []
                (empty? p) paras
                :else (cons p paras))]
      (let [lines (strng/split p #"\n")
            [prop leafs]
            (split-at (->> lines (take-while (:props-keep cfg))
                           count ((:props-split-inc cfg)))
                      lines)]
        [(-> (map #(strng/replace % (:props-prefix cfg) "") prop)
             ((:props-extract cfg))
             parse-properties)
         (cons (strng/join " " leafs) paras)]))))

(defn parse-subtree-shallow [level parsed-chunks]
  (map #(let [[props leafs] (get-leafs-and-properties
                             (second %) subtree-props-parse-cfg)]
          {:headline (first %)
           :properties props
           :level (inc level)
           :leafs leafs})
       parsed-chunks))

(defn parse-subtree
  "This is an inner recursion version of the `org-outline` function,
  and the only difference is the properties parse syntax."
  ([level shallow title s]
   (let [[intro & chunks] (parse-subtree-chunks level s)
         subtrees (map parse-chunk chunks)
         [props leafs] (get-leafs-and-properties
                        intro subtree-props-parse-cfg)]
     {:headline title
      :properties props
      :level level
      :leafs leafs
      :subtrees
      (if shallow
        (parse-subtree-shallow level subtrees)
        (map (partial apply (partial parse-subtree (inc level) false))
             subtrees))})))

(defn org-outline
  ([path] (org-outline 0 false (org-file-name path) (slurp path)))
  ([path shallow] (org-outline 0 true (org-file-name path) (slurp path)))
  ([level title s] (org-outline level false title s))
  ([level shallow title s]
   (let [[intro & chunks] (parse-subtree-chunks level s)
         subtrees (map parse-chunk chunks)
         root-prop-line #"^\s*#\+"
         [props leafs] (get-leafs-and-properties
                        intro {:props-extract identity
                               :props-start root-prop-line
                               :props-keep (partial re-find root-prop-line)
                               :props-prefix root-prop-line
                               :props-split-inc identity})]
     {:headline title
      :properties props
      :level level
      :leafs leafs
      :subtrees
      (if shallow
        (parse-subtree-shallow level subtrees)
        (map (partial apply (partial parse-subtree (inc level) false))
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
     (strng/join "\n" (concat
                       frontmatter
                       (when (seq subtrees)
                         (map serialize-outline subtrees)))))))
