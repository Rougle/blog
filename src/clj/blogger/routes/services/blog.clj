(ns blogger.routes.services.blog
  (:require [blogger.db.core :as db]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [clojure.java.io :as io])
  (:import (java.io FileInputStream ByteArrayOutputStream ByteArrayInputStream)))

(defn get-entries []
  (response/ok (db/get-entries)))

(defn get-entry [id]
  (if-let [entry (db/get-entry {:id id})]
    (response/ok entry)
    (response/not-found {:message "Couldn't find entry with given id"})))

(defn create-entry! [header summary content author]
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
      (response/ok (db/get-entry {:id id}))
      (response/not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

;;TODO Do in a transaction instead
(defn delete-entry! [id]
  (try
    (db/delete-entry-images! {:entry_id id})
    (if (= 1 (db/delete-entry! {:id id}))
      (response/no-content)
      (response/not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn file->byte-array [file]
  (with-open [input (FileInputStream. file)
              buffer (ByteArrayOutputStream.)]
    (io/copy input buffer)
    (.toByteArray buffer)))

(defn upload-single-image! [id file]
  (log/debug file)
  (let [{:keys [filename tempfile content-type]} file]
    (try
      (db/upload-image! {:name filename
                         :type content-type
                         :entry_id id
                         :data (file->byte-array tempfile)})
      {:filename filename :uploaded true}
      (catch Exception e
        (log/error e)
        {:filename filename :uploaded false}))))

(defn upload-images! [id file]
  (log/debug (vector? file))
  (try
    (if (vector? file)
      (response/ok {:message (map #(upload-single-image! id %) file)})
      (response/ok {:message [(upload-single-image! id file)]}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn delete-image! [name]
  (try
    (db/delete-image! {:name name})
    (response/ok {:message "Success"})
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn get-entry-images [id]
  (response/ok (db/get-entry-images {:entry_id id})))

(defn get-images []
  (response/ok (db/get-images)))

(defn get-image [name]
  (if-let [{:keys [data type]} (db/get-image {:name name})]
    (-> (ByteArrayInputStream. data)
        (response/ok)
        (response/content-type type))
    (response/not-found {:message "Not found"})))
