(ns com.tylerkindy.server-admin.healthcheck
  (:require [clj-http.client :as client])
  (:import [java.time Duration]))

(defn healthy? [port]
  (let [response (try
                   (client/get (str "http://localhost:" port))
                   (catch Exception _ {}))]
    (= (:status response) 200)))

(def timeout (Duration/ofSeconds 30))

; TODO: remove .toMillis once server is on Java 19
(def sleep-length (-> (Duration/ofSeconds 1)
                      .toMillis))

(defn wait-until-healthy [port]
  (let [start (System/nanoTime)]
    (loop []
      (let [elapsed (Duration/ofNanos (- (System/nanoTime) start))]
        (and (< (.compareTo elapsed timeout) 0)
             (or (healthy? port)
                 (do
                   (Thread/sleep sleep-length)
                   (recur))))))))
