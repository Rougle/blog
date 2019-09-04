(ns blogger.core
  (:require
    [reagent.core :as r]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [blogger.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string]
    [blogger.components.blog :as blog]
    [blogger.components.auth :as auth]
    [blogger.components.contact :as contact]
    [blogger.components.about :as about])
  (:import goog.History))

(defonce session (r/atom {:page :list-entries}))
(defonce match (r/atom nil))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page (:page @session)) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:div
     [:h1.site-header "Devblog"]
     [:hr.header-separator]
     [:nav.navbar>div.container
      [:div#nav-menu.navbar-menu
       {:class (when @expanded? :is-active)}
       [:div.navbar-start
        [nav-link "#/" "Blog" :list-entries]
        [nav-link "#/about" "About" :about]
        [nav-link "#/contact" "Contact" :contact]]
       [(auth/user-logout)]]]


     [:hr.header-separator]]))

(defn about-page []
  [:section.section>div.container>div.content
   [(about/about)]])

(defn contact-page []
  [:section.section>div.container>div.content
   [(contact/contact)]])

(defn entries-list []
  [:section.section>div.container>div.content
   [(blog/entries-list)]])

(defn entry-view []
  (let [{{:keys [id]} :path-params} @match]
    [:section.section>div.container>div.content
     [(blog/entry-view id)]]))

(defn new-entry []
  [:section.section>div.container>div.content
   [(blog/new-entry-form)]])

(defn edit-entry []
  (let [{{:keys [id]} :path-params} @match]
    [:section.section>div.container>div.content
     [(blog/edit-entry-form id)]]))

(defn register []
  [:section.section>div.container>div.content
   [(auth/register-form)]])

(defn login []
  [:section.section>div.container>div.content
   [(auth/login-form)]])

(def pages
  {:list-entries #'entries-list
   :view-entry #'entry-view
   :edit-entry #'edit-entry
   :post-entry #'new-entry
   :register #'register
   :login #'login
   :about #'about-page
   :contact #'contact-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :list-entries]
     ["/entry/view/:id" :view-entry]
     ["/entry/edit/:id" :edit-entry]
     ["/entry/post" :post-entry]
     ["/auth/register" :register]
     ["/auth/login" :login]
     ["/about" :about]
     ["/contact" :contact]]))

;;TODO There should be a better way to pass path params to components
(defn match-route [uri]
  (let [matched-route (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
                           (reitit/match-by-path router))]
      (do
        (reset! match matched-route)
        (->> matched-route :data :name)
    )))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (swap! session assoc :page (match-route (.-token event)))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(swap! session assoc :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (ajax/load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
