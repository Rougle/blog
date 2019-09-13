(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [goog.events :as gev]
            [markdown.core :refer [md->html]]
            [blogger.components.common :as c]
            [blogger.components.session :as s])
  (:import goog.net.IframeIo
           goog.net.EventType
           [goog.events EventType]))

;; TODO parse date into human-friendly form
(defn entry-preview [entry]
  (fn []
    (let [{:keys [id header summary first_name last_name created]} entry]
      [:div.blog-preview
       [:a {:href (str "#/entry/view/" id)}
        [:h1 header]]
       [:p summary]
       [:p [:small (str "By " first_name " " last_name " on " created)]]])))

(defn entries-list []
  (let [entries (atom [])]
  (ajax/GET "/api/blog/entries" {:handler #(reset! entries %)})
  (fn []
    [:div
     (if (:token @s/session)
       [:div.form-btn-group
        [:a {:href "#/entry/post"}
         [:button.btn.btn-primary
          "New Entry"]]])
     (for [entry @entries]
       (let [{:keys [id]} entry]
         [:div {:key id}
          [entry-preview entry]
          [:hr]]))])))

(defn delete-entry! [id error]
  (reset! error {})
  (ajax/DELETE (str "/api/blog/entry/" id)
               {:handler #(s/set-hash! "/")
                :error-handler #(reset! error {:message (:status-text %)})}))

(defn update-entry! [data fields editing? error]
  (reset! error {})
  (ajax/POST (str "/api/blog/entry/" (:id @fields))
             {:params @fields
              :handler #(do
                          (reset! data %)
                          (reset! editing? false))
              :error-handler #(reset! error {:message (:status-text %)})}))

;; TODO Redirect only if upload was a success
(defn post-entry! [data fields error redirect?]
  (reset! error {})
  (swap! fields assoc :author (:username @s/session))
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler #(do
                          (reset! fields %)
                          (reset! data %)
                          (if redirect?
                            (s/set-hash! (str "/entry/view/" (:id %)))))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn entry-fields [fields]
  [:div
   [c/text-input "Header" :header "Enter a header" fields]
   [c/text-input "Summary - this will be only shown in preview" :summary "Enter a summary" fields]
   [c/textarea-input "Content" :content "Enter blog content in markup" fields]])

;;TODO Fix error handling
;;TODO Fix duplicate call
(defn upload-file! [upload-form-id entry-id status]
  (reset! status nil)
  (let [io (IframeIo.)]
    (gev/listen
      io goog.net.EventType.SUCCESS
      #(reset! status {:message "success"}))
    (gev/listen
      io goog.net.EventType.ERROR
      #(reset! status {:message "fail"}))
    (.setErrorChecker io #(= "error" (.getResponseText io)))
    (.sendFromForm
      io
      (.getElementById js/document upload-form-id)
      (str "/api/blog/entry/" entry-id "/image"))))

;; TODO Fix error/success messages
;; TODO Add image delete to edit page
;; (if (not (clojure.string/blank? (aget (.getElementById js/document input-id) "value")))
(defn upload-form [entry-id]
  (let [status (atom nil)
        form-id "upload-form"
        input-id "upload-input"]
    (fn []
      [:div
       [:form {:id form-id
               :enc-type "multipart/form-data"
               :method "POST"
               :target "dummy"}
        [:fieldset {:disabled (nil? entry-id)}
         [:label {:for "file"} "Select an image for upload "]
         [:input {:id input-id :name "file" :type "file" :multiple true}]]
        [:div.form-btn-group
         (if (not (nil? entry-id))
           [:button.btn.btn-primary
            {:on-click #(upload-file! form-id entry-id status)}
            "Upload"])]]
       [:iframe {:name "dummy" :id "dummy" :style { :display "none"}}]
       ])))

(defn edit-entry [data editing? error]
  (let [fields (atom @data)]
    (fn []
      [:div
       [:h2 "Edit entry"]
       [entry-fields fields]
       [:div.form-btn-group
        [:button.btn.btn-danger
         {:on-click #(if (:id fields)
                       (reset! editing? false)
                       (s/set-hash! "/"))}
         "Cancel"]
        [:div.form-btn-group
         (if (nil? (:id @data))
           [:button.btn.btn-primary
            {:on-click #(post-entry! data fields error false)}
            "Post"]
           [:button.btn.btn-primary
            {:on-click #(update-entry! data fields editing? error)}
            "Save"])]]
       [:div
        [:h6 "Add images"]
        [:p "You can refer them in markdown like this: \"img/example.jpg\" "]
        (if (nil? (:id @fields))
          [:p "You need to post the entry before adding images to it."])
        [(upload-form (:id @fields))]]])))

(defn view-entry [fields]
  (fn []
    [:div
     (let [{:keys [header content first_name last_name created]} @fields]
       [:div
        [:h1 header]
        [:p [:small (str "By " first_name " " last_name " on " created)]]
        [:div {:dangerouslySetInnerHTML {:__html (md->html content) }}]])]))

(defn entry [id]
  (let [data (atom nil)
        editing? (atom false)
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" id)
              {:handler #(reset! data %)
               :error-handler #(reset! error %)})
    (fn []
      [:div
       [(c/request-error error)]
       (if (and (:token @s/session) (not @editing?))
         [:div.form-btn-group
          [:button.btn.btn-danger
           {:on-click #(delete-entry! id error)}
           "Delete"]
          [:button.btn.btn-primary
           {:on-click #(reset! editing? true)}
           "Edit"]])
       (if @editing?
         [(edit-entry data editing? error)]
         [(view-entry data)])])))

;; TODO Common client-side/server-side checks to cljc
;; TODO Buttons into reusable components
(defn create-entry []
  (let [fields (atom {})
        data (atom nil)
        editing? (atom true)
        error (atom {})]
    (fn []
      [:div
       [(c/request-error error)]
       [:div
        [(c/request-error error)]
        (if (and (:token @s/session) (not @editing?))
          [:div.form-btn-group
           [:button.btn.btn-danger
            {:on-click #(delete-entry! (:id @data) error)}
            "Delete"]
           [:button.btn.btn-primary
            {:on-click #(reset! editing? true)}
            "Edit"]])
        (if @editing?
          [(edit-entry data editing? error)]
          [(view-entry data)])]])))
