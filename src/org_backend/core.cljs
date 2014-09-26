(ns org-backend.core)

(def *slurp-fn* (atom nil))
(defn set-slurp-fn! [fn]
  (reset! *slurp-fn* fn))

(defn slurp [path]
  (try (@*slurp-fn* path)
       (catch js/Object e
         (throw (str "You must set-slurp-fn! with a function taking a valid "
                     "string path.")))))
