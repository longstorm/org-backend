(defproject longstorm/org-backend "0.2.0"
  :description "Parse or store data in .org syntax from the JVM or Node.js"
  :url "https://github.com/longstorm/org-backend"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/longstorm/org-backend.git"}
  :aliases {"cleantest" ["do" "clean," "cljsbuild" "once," "test,"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test"]}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:target :nodejs
                                   :output-to "target/org-backend.js"
                                   :optimizations :simple}}]}
  :profiles {:dev {:plugins [[lein-cljsbuild "1.0.5"]
                             [com.cemerick/clojurescript.test "0.3.3"]]
                   :dependencies [[com.cemerick/double-check "0.6.1"]
                                  [com.cemerick/piggieback "0.2.0"]
                                  [org.clojure/tools.nrepl "0.2.10"]]
                   :repl-options {:nrepl-middleware
                                  [cemerick.piggieback/wrap-cljs-repl]}}})
