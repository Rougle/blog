(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [blogger.components.common :as c]))

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
     (for [entry @entries]
       (let [{:keys [id]} entry]
         [:div {:key id}
          [entry-preview entry]
          [:hr]]))])))

(defn entry-view [id]
  (let [data (atom nil)]
    (ajax/GET (str "/api/blog/entry/" id) {:handler #(reset! data %)})
    (fn []
      (let [{:keys [header summary content]} @data]
        [:div
         [:h1 header]
         [:p summary]
         [:p content]]))))

(defn post-entry! [fields]
  (ajax/POST "/api/blog/entry"
             {:params @fields
              :handler (reset! fields {})}))

;; TODO Client-side checks, error messages, redirect
;; TODO Setup session, get id from there
(defn new-entry-form []
  (let [fields (atom {})]
    (swap! fields assoc :author_id  #uuid"ceb56f18-77b8-4cc8-88ac-52aff9b5050e")
    (fn []
      [:div
       [:h2 "Create a new entry"]
       [:div
        [c/text-input "Header" :header "Enter a header" fields]
        [c/text-input "Summary" :summary "Enter a summary" fields]
        [c/textarea-input "Content" :content "Enter blog content" fields]]
       [:div.form-btn-group
        [:button.btn.btn-danger
         {:on-click #()}
         "Cancel"]
        [:button.btn.btn-primary
         {:on-click #(post-entry! fields)}
         "Post"]]])))

(defn update-entry! [id fields]
  (ajax/POST (str "/api/blog/entry/" id)
             {:params @fields
              :handler (reset! fields {})}))

;; TODO Client-side checks, error messages, redirect
(defn edit-entry-form [id]
  (let [fields (atom {})]
    (ajax/GET (str "/api/blog/entry/" id) {:handler #(reset! fields %)})
    (fn []
      [:div
       [:h2 "Edit entry"]
       [:div
        [c/text-input "Header" :header "Enter a header" fields]
        [c/text-input "Summary" :summary "Enter a summary" fields]
        [c/textarea-input "Content" :content "Enter blog content" fields]]
       [:div.form-btn-group
        [:button.btn.btn-danger
         {:on-click #()}
         "Cancel"]
        [:button.btn.btn-primary
         {:on-click #(update-entry! id fields)}
         "Post"]]])))
