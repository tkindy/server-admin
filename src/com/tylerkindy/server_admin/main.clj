(ns com.tylerkindy.server-admin.main
  (:require [clojure.string :as str]
            [com.tylerkindy.server-admin.ports :refer [pick-port]]
            [com.tylerkindy.server-admin.screen :refer [find-screen start-screen interrupt-child]]
            [com.tylerkindy.server-admin.caddy :as caddy]
            [com.tylerkindy.server-admin.healthcheck :refer [wait-until-healthy]]
            [com.tylerkindy.server-admin.config :as config])
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

(defn route-for-host? [route host]
  (= (get-in route [:match 0 :host 0])
     host))

(defn find-route [routes host]
  (->> routes
       (map-indexed vector)
       (filter (fn [[_ route]] (route-for-host? route host)))
       first
       first))

(defn build-route [host port]
  {:match [{:host [host]}]
   :handle [{:handler :reverse_proxy
             :upstreams [{:dial (str ":" port)}]}]})

(defn update-route [routes route-index host new-port]
  (assoc-in routes route-index (build-route host new-port)))

(defn add-route [routes host new-port]
  (concat routes [(build-route host new-port)]))

(defn update-routes [routes {:keys [host]} new-port]
  (let [route-index (find-route routes host)]
    (if route-index
      (update-route routes route-index host new-port)
      (add-route routes host new-port))))

(defn update-port [caddy-config app-config new-port]
  (update-in caddy-config
             [:apps :http :servers :srv0 :routes]
             update-routes app-config new-port))

(defn add-logger [caddy-config {:keys [name host]}]
  (assoc-in caddy-config
            [:apps :http :servers :srv0 :logs :logger_names host]
            name))

(defn set-log-file [caddy-config name]
  (assoc-in caddy-config
            [:logging :logs name]
            {:writer {:output :file
                      :filename (str "/var/log/websites/access-" name ".log")}
             :include [(str "http.log.access." name)]}))

(defn exclude-default [caddy-config name]
  (update-in caddy-config
             [:logging :logs :default :exclude]
             (fn [excluded] (conj (set excluded)
                                  (str "http.log.access." name)))))

(defn connect-logger [caddy-config name]
  (-> caddy-config
      (set-log-file name)
      (exclude-default name)))

(defn setup-logging [caddy-config {:keys [name] :as app-config}]
  (-> caddy-config
      (add-logger app-config)
      (connect-logger name)))

(defn build-caddy [new-port]
  (let [caddy-config (caddy/get-config)
        app-config (config/read-config)]
    (-> caddy-config
        (update-port app-config new-port)
        (setup-logging app-config))))

(defn swap-caddy [new-port]
  (caddy/load-config (build-caddy new-port)))

(defn -main [new-jar]
  (let [app-config (config/read-config)
        screen-name (:name app-config)
        old-screen (find-old-screen screen-name)
        new-port (pick-port)]
    (println "Starting new process on port" new-port)
    (start-screen screen-name (str "java -jar " new-jar " --port " new-port))

    (println "Waiting for new service to become healthy")
    (when (not (wait-until-healthy new-port))
      (println "Service didn't become healthy before timeout, stopping deploy")
      (System/exit 1))

    (println "Swapping reverse proxy to new service")
    (swap-caddy new-port)

    (when old-screen
      (println "Interrupting old process")
      (interrupt-child old-screen))

    (println "Done!")

    ; TODO: find non-daemon threads keeping the JVM alive to remove this call
    (System/exit 0)))
