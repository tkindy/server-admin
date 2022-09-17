(ns com.tylerkindy.server-admin.ports
  (:import [java.net Socket ConnectException]))

(defn port-taken? [port]
  (let [socket (try
                 (Socket. "127.0.0.1" port)
                 (catch ConnectException _ nil))]
    (when socket
      (.close socket))
    (boolean socket)))
