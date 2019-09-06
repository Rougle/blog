(ns blogger.routes.services.content
  (:require [blogger.db.core :as db]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]))

(def valid-content-ids ["about" "contact"])

(defn get-content [id]
  (if-let [content (db/get-content {:id id})]
    (response/ok content)
    (response/not-found {:message "Couldn't find content with given id"})))

(defn create-content! [id content]
  (if (not (contains? valid-content-ids id))
    (response/precondition-failed [:message "Invalid content id"])
    (try
      (db/create-content!
        {:id            id
         :content       content})
        (response/ok (db/get-content {:id id}))
      (catch Exception e
        (log/error e)
        (response/internal-server-error {:message "Internal server error"})))))

(defn update-content! [id content]
  (try
    (if (= 1 (db/update-content! {:id id :content content}))
      (response/ok (db/get-content {:id id}))
      (response/not-found {:message "Couldn't find content with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))
