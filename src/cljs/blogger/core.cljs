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
    [blogger.components.blog :as blog])
  (:import goog.History))

(defonce session (r/atom {:page :home}))
(defonce match (r/atom nil))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page (:page @session)) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "blogger"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/entries" "Blog" :list-entries]
       [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

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

;;TODO Delete entry

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs (:docs @session)]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(def pages
  {:home #'home-page
   :list-entries #'entries-list
   :view-entry #'entry-view
   :edit-entry #'edit-entry
   :post-entry #'new-entry
   :about #'about-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/entries" :list-entries]
     ["/entry/view/:id" :view-entry]
     ["/entry/edit/:id" :edit-entry]
     ["/entry/post" :post-entry]
     ["/about" :about]]))

;;TODO There should be a better way to parse path params
;;TODO Fix duplicate function call
(defn match-route [uri]
  (reset! match (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)))
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)
       :data
       :name))
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
