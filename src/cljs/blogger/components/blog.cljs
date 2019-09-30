(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [markdown.core :refer [md->html]]
            [blogger.components.common :as c]
            [blogger.components.session :as s]))

(defn entry-preview [entry]
  (fn []
    (let [{:keys [id header summary created]} entry]
      [:div.blog-preview
       (if (not (nil? entry))
         [:div.preview-date (c/parse-date-string created)])
       [:a {:href (str "#/entry/view/" id)}
        [:h1 header]]
       [:p summary]])))

(defn add-entry-button []
  [:a.fixed-float {:href "#/entry/post"}
    [c/primary-button nil "New Entry"]])

(defn entries-list []
  (let [entries (atom [])]
    (ajax/GET "/api/blog/entries" {:handler #(reset! entries %)})
    (fn []
      [:div
       (if (:token @s/session)
         [add-entry-button])
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

(defn upload-images! [input-id entry-id]
  (let [results (atom {})
        form-data (let [f-d (js/FormData.)
                        files (.-files (.getElementById js/document input-id))
                        name (.-name (.getElementById js/document input-id))]
                    (doseq [file-key (.keys js/Object files)]
                      (.append f-d name (aget files file-key)))
                    f-d)]
    (if (not (zero? (.-length (.-files (.getElementById js/document input-id)))))
      (ajax/POST (str "/api/blog/entry/" entry-id "/image")
                 {:body form-data
                  :response-format :json
                  :keywords? true
                  :handler #(reset! results (:message %))
                  :error-handler #(reset! results (:message %))}))))

(defn update-entry! [fields data upload-input-id error]
  (reset! error {})
  (ajax/POST (str "/api/blog/entry/" (:id @fields))
             {:params @fields
              :handler #(do
                          (upload-images! upload-input-id (:id %))
                          (reset! data %)
                          (reset! fields %))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn post-entry! [fields data upload-input-id error]
  (reset! error {})
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler #(do
                          (upload-images! upload-input-id (:id %))
                          (reset! data %)
                          (reset! fields %))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn upload-form [input-id]
  (fn []
    [:div
     [:fieldset
      [:label {:for "file"} "Select images for upload "]
      [:input
       {:name "file"
        :id input-id
        :type "file"
        :multiple true}]]]))

;;TODO Fix error message
(defn delete-image! [image images]
  (ajax/DELETE (str "/api/blog/image/" (:name image))
               {:handler       #(swap! images
                                       (fn [imgs] (remove (fn [img] (= image img)) imgs)))
                :error-handler #(print "Could not delete image")}))

(defn image-list [image images]
  (fn []
    [:div
     [:div (:name image)
      [:i.material-icons.text-icon.clickable
       {:on-click #(delete-image! image images)} "delete"]]]))

(defn entry-images [entry-id]
  (let [images (atom {})
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" entry-id "/images")
              {:handler #(reset! images %)
               :error-handler #(reset! error %)})
    (fn []
      [:div
       [:h6 "Images attached to the entry:"]
       (for [image @images]
         [:div {:key (:name image)}
          [(image-list image images)]])
       ])))

(defn entry-fields [fields data upload-input-id error]
  [:div
   [:h2 "Entry"]
   [c/text-input "Header" :header "Enter a header" fields]
   [c/text-input "Summary - this will be only shown in preview" :summary "Enter a summary" fields]
   [c/textarea-input "Content" :content "Enter blog content in markup" fields]
   [:div.form-btn-group
    [c/danger-button #(s/set-hash! "/") "Cancel"]
    (if (nil? (:id @fields))
      [c/primary-button #(post-entry! fields data upload-input-id error) "Save"]
      [c/primary-button #(update-entry! fields data upload-input-id error) "Save"])]])

(defn entry-form [data error]
  (let [fields (atom @data)
        upload-input-id "image-input"]
    (fn []
      [:div
       [(c/request-error error)]
       [entry-fields fields data upload-input-id error]
       [:div
        [:h6 "Article Images"]
        [:p "You can refer them in markdown like this: \"api/blog/image/example.jpg\" "]
        [(upload-form upload-input-id)]
        [:hr]
        (if (:id @data)
          [entry-images (:id @data)])]])))

(defn view-entry [id]
  (let [data (atom {})
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" id)
              {:handler #(reset! data %)
               :error-handler #(reset! error %)})
    (fn []
      (let [{:keys [header content created]} @data]
        [:div
         [(c/request-error error)]
         (if (:token @s/session)
           [:div.form-btn-group
            [c/danger-button #(delete-entry! id error) "Delete"]
            [c/primary-button #(s/set-hash! (str "/entry/edit/" id)) "Edit"]])
         [:h1 header]
         (if (not (nil? created))
           [:p [:small (c/parse-date-string created)]])
         [:div {:dangerouslySetInnerHTML {:__html (md->html content) }}]]))))

(defn edit-entry [id]
  (let [data (atom nil)
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" id)
              {:handler #(reset! data %)
               :error-handler #(reset! error %)})
    (fn []
      [:div
       [(entry-form data error)]])))

;; TODO Common client-side/server-side checks to cljc
(defn create-entry []
  (let [data (atom nil)
        error (atom {})]
    (fn []
      [:div
       [(entry-form data error)]])))
