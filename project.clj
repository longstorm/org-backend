(defproject longstorm/org-backend "0.1.7"
  :description "Parse or store data in .org syntax from the JVM or Node.js"
  :url "https://github.com/longstorm/org-backend"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/longstorm/org-backend.git"}
  :aliases {"cleantest" ["do" "clean," "cljsbuild" "once," "test,"]
            "autotest" ["do" "clean," "cljsbuild" "auto" "test"]}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]]
  :profiles
  {:dev {:dependencies [[com.cemerick/double-check "0.5.7-SNAPSHOT"]]
         :hooks [leiningen.cljsbuild]
         :plugins [[lein-cljsbuild "1.0.3"]
                   [com.cemerick/clojurescript.test "0.3.0"]]}})
