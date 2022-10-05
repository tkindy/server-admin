(ns com.tylerkindy.server-admin.main
  (:require [clojure.string :as str]
            [com.tylerkindy.server-admin.ports :refer [pick-port]]
            [com.tylerkindy.server-admin.screen :refer [find-screen start-screen interrupt-child]]
            [com.tylerkindy.server-admin.caddy :refer [update-config]]
            [com.tylerkindy.server-admin.healthcheck :refer [wait-until-healthy]])
  (:gen-class))

(defn base-screen-name [new-name]
  (-> new-name
      (str/split #"-")
      first))

(defn find-old-screen [new-name]
  (-> new-name
      base-screen-name
      find-screen
      first))

(defn swap-caddy [id new-port]
  (update-config id "dial" (str "\":" new-port "\"")))

(defn -main [new-jar]
  (let [screen-name "synchro"
        old-screen (find-old-screen screen-name)
        new-port (pick-port)]
    (println "Starting new process on port" new-port)
    (start-screen screen-name (str "java -jar " new-jar " --port " new-port))

    (println "Waiting for new service to become healthy")
    (when (not (wait-until-healthy new-port))
      (println "Service didn't become healthy before timeout, stopping deploy")
      (System/exit 1))

    (println "Swapping reverse proxy to new service")
    (swap-caddy "synchro_upstream" new-port)

    (println "Interrupting old process")
    (interrupt-child old-screen)

    (println "Done!")

    ; TODO: find non-daemon threads keeping the JVM alive to remove this call
    (System/exit 0)))
