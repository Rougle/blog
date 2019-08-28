(ns blogger.components.blog
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]))

(def entries (atom []))

;; TODO parse date into human-friendly form
(defn entry-preview [entry]
  (fn []
    (let [{:keys [id header summary first_name last_name created]} entry]
        [:div.blog-preview
         [:a {:href (str "#/blog/entry/" id)}
          [:h1 header]]
         [:p summary]
         [:p [:small (str "By " first_name " " last_name " on " created)]]])))

(defn entries-list []
  (ajax/GET "/api/blog/entries" {:handler #(reset! entries %)})
  (fn []
    [:div
     (for [entry @entries]
       (let [{:keys [id]} entry]
         [:div {:key id}
          [entry-preview entry]
          [:hr]]))]))

(defn entry [id]
  (let [data (atom nil)]
    (ajax/GET (str "/api/blog/entry/" id) {:handler #(reset! data %)})
    (fn []
      (let [{:keys [header content]} @data]
        [:div
          [:h1 header]
          [:p content]]))))
