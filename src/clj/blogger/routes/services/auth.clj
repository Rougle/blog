(ns blogger.routes.services.auth
  (:require [blogger.db.core :as db]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]))

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

(defn register! [session pass username first_name last_name]
  (try
    (let [id (java.util.UUID/randomUUID)]
      (log/debug session)
      (log/debug pass username first_name last_name)
      (db/create-user!
        (-> {:id id
             :username username
             :first_name first_name
             :last_name last_name
             :pass pass}
            (update :pass hashers/encrypt)))
      (-> {:result :ok}
          (response/ok)
          (assoc :session (assoc session :identity (:id id)))))
    (catch Exception e
      (handle-registration-error e))))

(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))

(defn authenticate [[username pass]]
  (log/debug username pass)
  (when-let [user (db/get-user {:username username})]
    (when (hashers/check pass (:pass user))
      username)))

(defn login! [session auth]
  (log/debug auth)
  (if-let [username (authenticate (decode-auth auth))]
    (-> {:result :ok}
        (response/ok)
        (assoc :session (assoc session :identity username)))
    (response/unauthorized {:result :unauthorized
                            :message "login failure"})))

(defn logout! []
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))
