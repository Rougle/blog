(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [markdown.core :refer [md->html]]
            [blogger.components.common :as c]
            [blogger.components.session :as s]))

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
           [c/primary-button nil "New Entry"]]])
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
(defn post-entry! [data fields error]
  (reset! error {})
  (swap! fields assoc :author (:username @s/session))
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler #(do
                          (reset! fields %)
                          (reset! data %))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn upload-images! [input-id entry-id results]
  (let [form-data (let [f-d (js/FormData.)
                        files (.-files (.getElementById js/document input-id))
                        name (.-name (.getElementById js/document input-id))]
                    (doseq [file-key (.keys js/Object files)]
                      (.append f-d name (aget files file-key)))
                    f-d)]
        (ajax/POST (str "/api/blog/entry/" entry-id "/image")
                   {:body form-data
                    :response-format :json
                    :keywords? true
                    :handler #(reset! results (:message %))
                    :error-handler #(reset! results (:message %))})))

(defn upload-results [results]
  (fn []
    [:div
     (for [result @results]
       (let [{:keys [filename uploaded]} result]
         [:div {:key filename}
          (if uploaded
            [:div (str filename " - Success")]
            [:div (str filename " - Failure")])]))]))

(defn upload-form [entry-id]
  (let [results (atom {})
        input-id "image-input"]
    (fn []
      [:div
       [:fieldset {:disabled (nil? entry-id)}
        [:label {:for "file"} "Select an image for upload "]
        [:input {:name "file" :id input-id :type "file" :multiple true }]]
       [(upload-results results)]
       (if (not (nil? entry-id))
         [:div.form-btn-group
          [c/primary-button #(upload-images! input-id entry-id results) "Upload"]])])))

(defn entry-fields [fields]
  [:div
   [c/text-input "Header" :header "Enter a header" fields]
   [c/text-input "Summary - this will be only shown in preview" :summary "Enter a summary" fields]
   [c/textarea-input "Content" :content "Enter blog content in markup" fields]])

;; TODO Add image view and delete
(defn edit-entry [data editing? error]
  (let [fields (atom @data)]
    (fn []
      [:div
       [:div
        [:h2 "Edit entry"]
        [entry-fields fields]
        [:div.form-btn-group
         (if (nil? (:id @data))
           [c/primary-button #(post-entry! data fields error) "Post"]
           [c/primary-button #(update-entry! data fields editing? error) "Save"])]]
       [:div
        [:h6 "Article Images"]
        [:p "You can refer them in markdown like this: \"img/example.jpg\" "]
        (if (nil? (:id @fields))
          [:p "No entry to attach images to. You need to post the entry before adding images to it."])
        [(upload-form (:id @fields))]]])))

(defn view-entry [fields]
  (let [{:keys [header content first_name last_name created]} @fields]
    (fn []
      [:div
       [:h1 header]
       [:p [:small (str "By " first_name " " last_name " on " created)]]
       [:div {:dangerouslySetInnerHTML {:__html (md->html content) }}]])))

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
          [c/danger-button #(delete-entry! id error) "Delete"]
          [c/primary-button #(reset! editing? true) "Edit"]])
       (if @editing?
         [(edit-entry data editing? error)]
         [(view-entry data)])])))

;; TODO Common client-side/server-side checks to cljc
;; TODO Buttons into reusable components
(defn create-entry []
  (let [data (atom nil)
        editing? (atom true)
        error (atom {})]
    (fn []
      [:div
       [(c/request-error error)]
       (if (and (:token @s/session) (not @editing?))
         [:div.form-btn-group
          [c/danger-button #(delete-entry! (:id @data) error) "Delete"]
          [c/primary-button #(reset! editing? true) "Edit"]])
       (if @editing?
         [(edit-entry data editing? error)]
         [(view-entry data)])])))
