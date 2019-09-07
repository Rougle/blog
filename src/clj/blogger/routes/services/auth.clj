(ns blogger.routes.services.auth
  (:require [blogger.db.core :as db]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.sign.util :refer [to-timestamp]]
            [clj-time.core :as t]
            [blogger.config :refer [env]]
            [clojure.tools.logging :as log]))

(defn get-users []
  (db/get-users))

(defn register! [username pass secret first_name last_name]
  (if (not (= secret (:register-secret env)))
    (response/forbidden {:message "Secret didn't match"})
    (try
      (db/create-user!
        {:username username
         :first_name first_name
         :last_name last_name
         :pass (hashers/encrypt pass)})
      (response/ok)
      (catch Exception e
        (response/internal-server-error
          {:message "Server error occurred while adding user. The username might be taken?"})))))

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

(defn create-auth-token [credentials]
  (let [[ok? res] (authenticate credentials)
        exp (-> (t/plus (t/now) (t/days 1)) (to-timestamp))
        jwt-secret (:jwt-secret env)
        jwt-alg (:jwt-alg env)]
    (if ok?
      [true {:token (jwt/sign res jwt-secret {:alg jwt-alg :exp exp})}]
      [false res])))

(defn login! [req]
  (let [[ok? res] (create-auth-token (parse-authorization req))]
    (if ok?
      (response/ok res)
      (response/forbidden res))))
