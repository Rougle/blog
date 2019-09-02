(ns blogger.components.auth
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [ajax.core :as ajax]
            [blogger.components.common :as c]
            [goog.crypt.base64 :as b64]
            [clojure.string :as string]))

;; TODO Move to common
(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))


(defn register! [fields error]
  (reset! error {})
  (ajax/POST "/api/auth/register"
             {:params        @fields
              :handler       #(do
                                (reset! fields {})
                                (set-hash! (str "/entries")))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn register-form []
  (let [fields (atom {})
        error (atom {})]
    (fn []
      [:div
       (when-let [message (:message @error)]
         [:div.alert.alert-danger (str message " - Check network-tab for details.")])
       [:h2 "Register"]
       [:div
        [c/text-input "First name" :first_name "Enter first name" fields]
        [c/text-input "Last name" :last_name "Enter last name" fields]
        [c/text-input "Username" :username "Enter username" fields]
        [c/password-input "Password" :pass "Enter password" fields]
        [:div.form-btn-group
         [:a {:href "#/entries"}
          [:button.btn.btn-danger
           "Cancel"]]
         [:button.btn.btn-primary
          {:on-click #(register! fields error)}
          "Register"]]]])))

(defn encode-auth [user pass]
  (->> (str user ":" pass) (b64/encodeString) (str "Basic ")))

(defn login! [fields error]
  (let [{:keys [:username :pass]} @fields]
    (reset! error {})
    (ajax/POST "/api/auth/login"
               {:headers       {"Authorization" (encode-auth (string/trim username) pass)}
                :handler       #(do
                                  (reset! fields {})
                                  (session/put! :identity username)
                                  (set-hash! (str "/entries")))
                :error-handler #(reset! error {:message (:status-text %)})})))

(defn login-form []
  (let [fields (atom {})
        error (atom {})]
    (fn []
      [:div
       (when-let [message (:message @error)]
         [:div.alert.alert-danger (str message " - Check network-tab for details.")])
       [:h2 "Login"]
       [:div
        [c/text-input "Username" :username "Enter username" fields]
        [c/password-input "Password" :pass "Enter password" fields]
        [:div.form-btn-group
         [:a {:href "#/entries"}
          [:button.btn.btn-danger
           "Cancel"]]
         [:button.btn.btn-primary
          {:on-click #(login! fields error)}
          "Login"]]]])))


(defn user-logout []
  (if-let [id (session/get :identity)]
    [:ul.nav.navbar-nav.pull-xs-right
     [:li.nav-item
      [:a.dropdown-item.btn
       {:on-click #(ajax/POST
                     "/api/auth/logout"
                     {:handler (fn [] (session/remove! :identity))})}
       [:i.fa.fa-user] " " id " | sign out"]]]))