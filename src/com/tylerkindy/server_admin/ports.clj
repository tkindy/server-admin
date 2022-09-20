(ns com.tylerkindy.server-admin.ports
  (:import [java.net Socket ConnectException]))

(defn- port-free? [port]
  (let [socket (try
                 (Socket. "127.0.0.1" port)
                 (catch ConnectException _ nil))]
    (when socket
      (.close socket))
    (not socket)))

(defn pick-port []
  (->> (iterate inc 8080)
       (filter port-free?)
       first))
