(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
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
     [:div.form-btn-group
      [:a {:href "#/entry/post"}
       [:button.btn.btn-primary
        "New Entry"]]]
     (for [entry @entries]
       (let [{:keys [id]} entry]
         [:div {:key id}
          [entry-preview entry]
          [:hr]]))])))

(defn delete-entry! [id error]
  (reset! error {})
  (ajax/DELETE (str "/api/blog/entry/" id)
             {:handler #(s/set-hash! "/entries")
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn entry-view [id]
  (let [data (atom nil)
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" id) {:handler #(reset! data %)})
    (fn []
      (let [{:keys [header summary content first_name last_name created]} @data]
        (when-let [message (:message @error)]
          [:div.alert.alert-danger (str message " - Check network-tab for details.")])
        [:div
         [:div.form-btn-group
          [:button.btn.btn-danger
           {:on-click #(delete-entry! id error)}
           "Delete"]
          [:a {:href (str "#/entry/edit/" id)}
           [:button.btn.btn-primary
            "Edit"]]]
         [:div
          [:h1 header]
          [:p [:small (str "By " first_name " " last_name " on " created)]]
          [:p summary]
          [:p content]]]))))

(defn post-entry! [fields error]
  (reset! error {})
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler #(do
                          (reset! fields {})
                          (s/set-hash! (str "/entry/view/" (:id %))))
              :error-handler #(reset! error {:message (:status-text %)})}))

;; TODO Common client-side/server-side checks to cljc
(defn new-entry-form []
  (let [fields (atom {})
        error (atom {})]
    (swap! fields assoc :author (:username @s/session))
    (fn []
      [:div
       (when-let [message (:message @error)]
         [:div.alert.alert-danger (str message " - Check network-tab for details.")])
       [:h2 "Create a new entry"]
       [:div
        [c/text-input "Header" :header "Enter a header" fields]
        [c/text-input "Summary" :summary "Enter a summary" fields]
        [c/textarea-input "Content" :content "Enter blog content" fields]]
       [:div.form-btn-group
        [:a {:href "#/entries"}
         [:button.btn.btn-danger
          "Cancel"]]
        [:button.btn.btn-primary
         {:on-click #(post-entry! fields error)}
         "Post"]]])))

(defn update-entry! [id fields error]
  (reset! error {})
  (ajax/POST (str "/api/blog/entry/" id)
             {:params @fields
              :handler #(do
                          (reset! fields {})
                          (s/set-hash! (str "/entry/view/" id)))
              :error-handler #(reset! error {:message (:status-text %)})}))

;; TODO Client-side checks
(defn edit-entry-form [id]
  (let [fields (atom {})
        error (atom {})]
    (ajax/GET (str "/api/blog/entry/" id) {:handler #(reset! fields %)})
    (fn []
      [:div
       [:h2 "Edit entry"]
       [:div
        [c/text-input "Header" :header "Enter a header" fields]
        [c/text-input "Summary" :summary "Enter a summary" fields]
        [c/textarea-input "Content" :content "Enter blog content" fields]]
       [:div.form-btn-group
        [:a {:href (str "#/entry/view/" id)}
         [:button.btn.btn-danger
          "Cancel"]]
        [:button.btn.btn-primary
         {:on-click #(update-entry! id fields error)}
         "Post"]]])))
