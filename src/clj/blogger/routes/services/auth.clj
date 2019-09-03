(ns blogger.routes.services.auth
  (:require [blogger.db.core :as db]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [buddy.sign.jws :as jws]
            [buddy.sign.jwt :as jwt]
            [buddy.sign.util :refer [to-timestamp]]
            [buddy.core.keys :as ks]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clj-time.core :as t]))

(defn get-users []
  (db/get-users))

(defn handle-registration-error [e]
  (if (and
        (instance? java.sql.SQLException e)
        (-> e (.getNextException)
            (.getMessage)
            (.startsWith "ERROR: duplicate key value")))
    (response/precondition-failed
      {:result :error
       :message "user with the selected ID already exists"})
    (do
      (log/error e)
      (response/internal-server-error
        {:result :error
         :message "server error occurred while adding the user"}))))

(defn register! [username pass first_name last_name]
  (try
    (db/create-user!
      {:username username
       :first_name first_name
       :last_name last_name
       :pass (hashers/encrypt pass)})
    (-> {:result :ok}
        (response/ok))
    (catch Exception e
      (handle-registration-error e))))

(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))

(defn authenticate [credentials]
  (let [user (db/get-user {:username (:username credentials)})
        unauthed [false {:message "Invalid username or password"}]]
    (if user
      (if (hashers/check (:pass credentials) (:pass user))
        [true {:user (dissoc user :pass)}]
        unauthed)
      unauthed)))

(defn parse-authorization [req]
  (let [[username pass] (decode-auth (-> req :parameters :header :authorization ))]
    {:username username :pass pass}))

;;TODO Secret and alg options to env var
(defn create-auth-token [auth-conf credentials]
  (let [[ok? res] (authenticate credentials)
        exp (-> (t/plus (t/now) (t/days 1)) (to-timestamp))]
    (if ok?
      [true {:token (jwt/sign res "mysupersecret" {:alg :hs512 :exp exp})}]
      [false res])))

(defn login! [req]
  (let [[ok? res] (create-auth-token (:auth-conf req) (parse-authorization req))]
    (if ok?
      {:status 201 :body res}
      {:status 401 :body res})))
