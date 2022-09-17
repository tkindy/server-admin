(ns com.tylerkindy.server-admin.sh
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn sh [command]
  (sh/sh "sh" "-c" (str/join " " command)))
