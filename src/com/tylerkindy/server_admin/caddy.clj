(ns com.tylerkindy.server-admin.caddy
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn get-config []
  (-> (client/get "http://localhost:2019/config/" {:as :json})
      :body))

(defn build-config-uri [id path]
  (str "http://localhost:2019/"
       (if id
         (str "id/" id "/")
         "config/")
       path))

(defn update-config
  ([path value] (update-config nil path value))
  ([id path value]
   (client/post (build-config-uri id path)
                {:body value
                 :content-type :json})))

(defn load-config [value]
  (client/post "http://localhost:2019/config/"
               {:body (json/generate-string value)
                :content-type :json}))
