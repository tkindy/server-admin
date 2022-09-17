(ns com.tylerkindy.server-admin.screen
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn- sh [command]
  (sh/sh "sh" "-c" (str/join " " command)))

(defn- parse-screen [line]
  (let [parts (-> line
                  str/trim
                  (str/split #"\t"))
        [pid name] (-> parts
                       (nth 0)
                       (str/split #"\." 2))]
    {:pid (Integer/parseInt pid)
     :name name}))

(defn- parse-screens [out]
  (let [screen-lines (->> out
                          str/split-lines
                          (filter #(str/starts-with? % "\t")))]
    (map parse-screen screen-lines)))

(defn find-screen [name]
  (let [out (-> (sh ["screen -ls" name])
                :out)]
    (parse-screens out)))

(defn start-screen [name command]
  (sh ["screen -S" name "-dm" command]))

(defn interrupt-child [screen]
  (sh ["pkill -2 --parent" (:pid screen)]))
