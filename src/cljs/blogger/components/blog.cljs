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

(defn entry-fields [fields]
  [:div
   [c/text-input "Header" :header "Enter a header" fields]
   [c/text-input "Summary - this will be only shown in preview" :summary "Enter a summary" fields]
   [c/textarea-input "Content" :content "Enter blog content in markup" fields]])

(defn edit-entry [data editing? error]
  (let [fields (atom @data)]
    (fn []
      [:div
       [:h2 "Edit entry"]
       [entry-fields fields]
       [:div.form-btn-group
        [:button.btn.btn-danger
         {:on-click #(reset! editing? false)}
         "Cancel"]
        [:button.btn.btn-primary
         {:on-click #(update-entry! data fields editing? error)}
         "Post"]]])))

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

(defn post-entry! [fields error]
  (reset! error {})
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler #(do
                          (reset! fields {})
                          (s/set-hash! (str "/entry/view/" (:id %))))
              :error-handler #(reset! error {:message (:status-text %)})}))

;; TODO Common client-side/server-side checks to cljc
(defn create-entry []
  (let [fields (atom {})
        error (atom {})]
    (swap! fields assoc :author (:username @s/session))
    (fn []
      [:div
       [(c/request-error error)]
       [:h2 "Create a new entry"]
       [entry-fields fields]
       [:div.form-btn-group
        [:a {:href "#/"}
         [:button.btn.btn-danger
          "Cancel"]]
        [:button.btn.btn-primary
         {:on-click #(post-entry! fields error)}
         "Post"]]])))
