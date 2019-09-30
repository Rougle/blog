(ns blogger.middleware
  (:require
    [blogger.env :refer [defaults]]
    [clojure.tools.logging :as log]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.util.http-response :as response]
    [blogger.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [blogger.config :refer [env]]
    [buddy.auth.accessrules :refer [restrict]]
    [buddy.auth :refer [authenticated? throw-unauthorized]]
    [buddy.auth.backends :as backends]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.auth.accessrules :refer [wrap-access-rules]]
    [blogger.config :refer [env]])
  (:import
           ))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (response/internal-server-error {:message "Internal server error"})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (response/forbidden {:message "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn on-error [request val]
  (response/forbidden {:message "Unauthorized"}))

(defn wrap-auth [handler]
  (let [backend (backends/jws {:secret (:jwt-secret env) :options {:alg (:jwt-alg env)}})]
    (-> handler
        (wrap-authentication backend))
    ))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)))
      wrap-internal-error))
