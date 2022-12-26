(ns com.tylerkindy.server-admin.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn read-config []
  (-> (io/reader "caddy-config.edn")
      PushbackReader.
      edn/read))
