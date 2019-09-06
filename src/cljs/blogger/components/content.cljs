(ns blogger.components.content
  (:require [ajax.core :as ajax]
            [markdown.core :refer [md->html]]
            [reagent.core :refer [atom]]
            [blogger.components.session :as s]
            [blogger.components.common :as c]))

(def edit-mode (atom false))
(def page-content (atom nil))

(defn update-content! [id fields error]
  (reset! error {})
  (ajax/POST (str "/api/content/" id)
             {:params        @fields
              :handler       #(do
                                (reset! fields {})
                                (reset! edit-mode false)
                                (reset! page-content (:content %)))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn edit-content-form [id]
  (let [content (atom {})
        error (atom {})]
    (ajax/GET (str "/api/content/" id) {:handler #(reset! content %)})
    (fn []
      [:div
       [:h2 (str "Edit  " id " page content")]
       [:div
        [c/textarea-input "Content" :content "Enter content in markdown format" content]]
       [:div.form-btn-group
        [:a {:href (str "#/" id)}
         [:button.btn.btn-danger
          {:on-click #(reset! edit-mode false)}
          "Cancel"]]
        [:button.btn.btn-primary
         {:on-click #(update-content! id content error)}
         "Post"]]])))

(defn content [id]
  (ajax/GET (str "/api/content/" id) {:handler #(reset! page-content (:content %))})
  (fn []
    [:div
      (if (and (:token @s/session) (not @edit-mode))
        [:div.form-btn-group
         [:button.btn.btn-primary
          {:on-click #(reset! edit-mode true)}
          "Edit"]])
      (if @edit-mode
        [(edit-content-form id)]
        [:div {:dangerouslySetInnerHTML {:__html (md->html @page-content) }}])]))
