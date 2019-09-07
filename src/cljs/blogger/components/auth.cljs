(ns blogger.components.auth
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [blogger.components.common :as c]
            [goog.crypt.base64 :as b64]
            [clojure.string :as string]
            [blogger.components.session :as s]
            [alandipert.storage-atom :refer [clear-local-storage!]]))

(defn register! [fields error]
  (reset! error {})
  (ajax/POST "/api/auth/register"
             {:params        @fields
              :handler       #(do
                                (reset! fields {})
                                (s/set-hash! (str "/auth/login")))
              :error-handler #(reset! error {:message (:status-text %)})}))

(defn register-form []
  (let [fields (atom {})
        error (atom {})]
    (fn []
      [:div
       [(c/request-error error)]
       [:h2 "Register"]
       [:div
        [c/text-input "First name" :first_name "Enter first name" fields]
        [c/text-input "Last name" :last_name "Enter last name" fields]
        [c/text-input "Username" :username "Enter username" fields]
        [c/password-input "Password" :pass "Enter password" fields]
        [c/password-input "Secret" :secret "Enter secret" fields]
        [:div.form-btn-group
         [:a {:href "#/"}
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
                                  (swap! s/session assoc :token (:token %))
                                  (swap! s/session assoc :username username)
                                  (s/set-hash! "/"))
                :error-handler #(reset! error {:message (:status-text %)})})))

(defn login-form []
  (let [fields (atom {})
        error (atom {})]
    (fn []
      [:div
       [(c/request-error error)]
       [:h2 "Login"]
       [:div
        [c/text-input "Username" :username "Enter username" fields]
        [c/password-input "Password" :pass "Enter password" fields]
        [:div.form-btn-group
         [:a {:href "#/"}
          [:button.btn.btn-danger
           "Cancel"]]
         [:button.btn.btn-primary
          {:on-click #(login! fields error)}
          "Login"]]]])))

(defn user-logout []
  (fn []
    (if (:token @s/session)
      [:a.navbar-item.sign-out
       {:on-click #(clear-local-storage!)}
       "sign out"])))