(ns blogger.routes.services.blog
  (:require [blogger.db.core :as db]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]))

(defn get-entries []
  (response/ok (db/get-entries)))

(defn get-entry [id]
  (if-let [entry (db/get-entry {:id id})]
    (response/ok entry)
    (response/not-found {:message "Couldn't find entry with given id"})))

(defn create-entry! [{:keys [author header summary content]}]
  (try
    (let [id (java.util.UUID/randomUUID)]
      (db/create-entry!
        {:id            id
         :author        author
         :created       (java.util.Date.)
         :last_modified (java.util.Date.)
         :header        header
         :summary       summary
         :content       content})
      (response/ok (db/get-entry {:id id})))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn update-entry! [id header summary content]
  (try
    (if (= 1 (db/update-entry! {:id id :header header :summary summary :content content}))
      (db/get-entry {:id id})
      (response/not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn delete-entry! [id]
  (try
    (if (= 1 (db/delete-entry! {:id id}))
      (response/no-content)
      (response/not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))
