(ns blogger.routes.services.blog
  (:require [blogger.db.core :as db]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [clojure.java.io :as io])
  (:import (java.io FileInputStream ByteArrayOutputStream)))

(defn get-entries []
  (response/ok (db/get-entries)))

(defn get-entry [id]
  (if-let [entry (db/get-entry {:id id})]
    (response/ok entry)
    (response/not-found {:message "Couldn't find entry with given id"})))

(defn create-entry! [author header summary content]
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

;;TODO Move images to img/entries/
(defn delete-img-resource! [name]
  (io/delete-file (str "resources/public/img/" name)))

(defn create-img-resource! [file]
  (with-open [w (io/output-stream (str "resources/public/img/" (:name file)))]
    (.write w (:data file))))

(defn delete-entry-images! [entry-id]
  (try
    (let [images (db/get-entry-images {:entry_id entry-id})]
        (db/delete-entry-images! {:entry_id entry-id})
        (doall (map #(delete-img-resource! (:name %)) images)))
    (catch Exception e
      (log/error e))))

;;TODO Do in a transaction instead
(defn delete-entry! [id]
  (try
    (delete-entry-images! id)
    (if (= 1 (db/delete-entry! {:id id}))
      (response/no-content)
      (response/not-found {:message "Couldn't find entry with given id"}))
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

;;TODO Run on startup
(defn populate-image-resources []
  (try
    (let [images (db/get-images)]
      (map #(create-img-resource! %) images))
    (catch Exception e
      (log/error e))))

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
      (create-img-resource! (db/get-image {:name filename}))
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
    (delete-img-resource! name)
    (response/ok {:message "Success"})
    (catch Exception e
      (log/error e)
      (response/internal-server-error {:message "Internal server error"}))))

(defn get-entry-images [id]
  (response/ok (db/get-entry-images {:entry_id id})))

(defn get-images []
  (response/ok (db/get-images)))