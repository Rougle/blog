(ns blogger.components.content
  (:require [ajax.core :as ajax]
            [markdown.core :refer [md->html]]
            [reagent.core :refer [atom]]
            [blogger.components.session :as s]
            [blogger.components.common :as c]))

(defn update-content! [id content fields editing? error]
  (reset! error {})
  (ajax/POST (str "/api/content/" id)
             {:params @fields
              :handler #(do
                          (reset! fields {})
                          (reset! editing? false)
                          (reset! content (:content %)))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn edit-content-form [id content editing? error]
  (let [fields (atom {:content @content})]
    (fn []
      [:div
       [:h2 (str "Edit " id " page content")]
       [:div
        [c/textarea-input "Content" :content "Enter content in markdown format" fields]]
       [:div.form-btn-group
        [:button.btn.btn-danger
         {:on-click #(reset! editing? false)}
         "Cancel"]
        [:button.btn.btn-primary
         {:on-click #(update-content! id content fields editing? error)}
         "Post"]]])))

(defn content [id]
  (let [editing? (atom false)
        content (atom nil)
        error (atom {})]
    (ajax/GET (str "/api/content/" id)
              {:handler #(reset! content (:content %))
               :error-handler #(reset! error %)})
    (fn []
      [:div
       [(c/request-error error)]
       (if (and (:token @s/session) (not @editing?))
         [:div.form-btn-group
          [:button.btn.btn-primary
           {:on-click #(reset! editing? true)}
           "Edit"]])
       (if @editing?
         [(edit-content-form id content editing? error)]
         [:div {:dangerouslySetInnerHTML {:__html (md->html @content) }}])])))

(defn header []
  (let [editing? (atom false)
        header (atom nil)
        fields (atom {})
        error (atom {})]
    (ajax/GET "api/content/header"
              {:handler #(reset! header (:content %))})
    (fn []
      [:div
       [(c/request-error error)]
       [:h1.site-header @header]
       (if @editing?
         [:div.header-edit-form
          [:form
           [c/input :text :content "Update header" fields]
           [:button.btn.btn-danger
            {:on-click #(reset! editing? false)}
            "Cancel"]
           [:button.btn.btn-primary
            {:on-click #(update-content! "header" header fields editing? error)}
            "Save"]]])
       (if (and (:token @s/session) (not @editing?))
         [:div.btn-header-edit
          [:button.btn.btn-primary
           {:on-click #(reset! editing? true)}
           "Edit"]])])))