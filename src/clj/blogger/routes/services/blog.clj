(ns blogger.routes.services.blog
  (:require [blogger.db.core :as db]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer :all]))

(defn get-entries []
  (ok (db/get-entries)))

(defn get-entry [id]
  (if-let [entry (db/get-entry {:id id})]
    (ok entry)
    (not-found {:message "Couldn't find entry with given id"})))

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
      (ok (db/get-entry {:id id})))
    (catch Exception e
      (log/error e))))

(defn update-entry! [id header summary content]
  (try
    (if (== 1 (db/update-entry! {:id id :header header :summary summary :content content}))
      (db/get-entry {:id id})
      (not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e))))

(defn delete-entry! [id]
  (try
    (if (== 1 (db/delete-entry! {:id id}))
      (no-content)
      (not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e))))
