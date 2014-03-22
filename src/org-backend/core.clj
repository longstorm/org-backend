(ns org-backend.core) 

(def *slurp-fn* (atom nil))

(defn slurp [path]
  (try (@*slurp-fn* path)
       (catch js/Object e
         (throw (str "You must set-slurp-fn! with a function taking a valid "
                     "string path and returning a string.")))))

(defn set-slurp-fn! [fn]
  (reset! *slurp-fn* fn))
