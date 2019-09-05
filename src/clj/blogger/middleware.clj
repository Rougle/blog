(ns blogger.middleware
  (:require
    [blogger.env :refer [defaults]]
    [cheshire.generate :as cheshire]
    [cognitect.transit :as transit]
    [clojure.tools.logging :as log]
    [blogger.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.session.cookie :refer [cookie-store]]
    [blogger.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [blogger.config :refer [env]]
    [ring-ttl-session.core :refer [ttl-memory-store]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [buddy.auth.accessrules :refer [restrict]]
    [buddy.auth :refer [authenticated? throw-unauthorized]]
    [buddy.auth.backends :as backends]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.auth.accessrules :refer [wrap-access-rules]]
    [buddy.core.keys :as ks]
    [clojure.java.io :as io]
    [buddy.sign.jwt :as jwt]
    [buddy.auth.protocols :as proto]
    [buddy.auth.http :as http]
    [blogger.config :refer [env]])
  (:import
           ))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn on-error [request]
  (error-page
    {:status 403
     :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-auth [handler]
  (fn [req]
    (if (:auth-user req)
      (handler req)
      {:status 302
       :headers {"Location " (str "/auth/login")}})))

(defn wrap-auth [handler]
  (let [backend (backends/jws {:secret (-> env :jwt-secret) :options {:alg (-> env :jwt-alg)}})]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
      wrap-internal-error))
