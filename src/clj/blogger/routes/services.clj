(ns blogger.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [blogger.middleware.formats :as formats]
    [blogger.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [blogger.routes.services.blog :as blog]
    [blogger.routes.services.auth :as auth]
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]))

(s/def ::id uuid?)
(s/def ::pass string?)
(s/def ::author_id uuid?)
(s/def ::header string?)
(s/def ::summary string?)
(s/def ::content string?)
(s/def ::username string?)
(s/def ::first_name string?)
(s/def ::last_name string?)
(s/def ::created string?)
(s/def ::last_modified string?)

(s/def ::new_user (s/keys :req-un [::pass ::username ::first_name ::last_name]))
(s/def ::user (s/keys :req-un [::id ::pass ::username ::first_name ::last_name]))
(s/def ::new_entry (s/keys :req-un [::author_id ::header ::summary ::content]))
(s/def ::entry (s/keys :req-un [::id ::author_id ::created ::last_modified ::header ::summary ::content ::first_name ::last_name]))
(s/def ::entries (s/coll-of ::entry))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "blog-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]


   ;; TODO Add authentication
   ;; TODO Having username and id is redundant. Rename username into id.
   ["/auth"
    {:swagger {:tags ["auth"]}}

     ["/register"
      {:post {:summary    "Registers new user"
              :parameters {:body ::new_user}
              :responses  {201 {:body {:res string?}}}
              :handler    (fn [request]
                            (let [{{{:keys [pass username first_name last_name]} :body} :parameters} request
                                  session (:session request)]
                            (auth/register! session pass username first_name last_name)))}}]
      ["/login"
       {:post {:summary    "Login user"
               :parameters {:header {:authorization string?}}
               :responses  {201 {:body {:res string?}}}
               :handler    (fn [request]
                             (let [{{{:keys [authorization]} :header} :parameters} request
                                    session (:session request)]
                             (auth/login! session authorization)))}}]
      ["/logout"
       {:post {:summary    "Logout user"
               :parameters {:header {:authorization string?}}
               :responses  {201 {:body {:res string?}}}
               :handler    (fn [request]
                               (auth/logout!))}}]]

  ;; TODO Consider splitting user from the blog-entry query or modifying the spec to better support it
  ;; TODO Consider renaming id -> entry_id for clarity
   ["/blog"
    {:swagger {:tags ["blog"]}}

    ["/entries"
     {:get {:summary   "Gets all entries"
            :responses {200 {:body ::entries}}
            :handler   (fn [_]
                         (blog/get-entries))}}]
    ["/entry"
     {:post {:summary    "Creates a new blog entry"
             :parameters {:body ::new_entry}
             :responses  {201 {:body ::entry}}
             :handler    (fn [{{:keys [body]} :parameters}]
                           (blog/create-entry! body))}}]

    ["/entry/:id"
     {:get    {:summary    "Gets a single entry by id"
               :parameters {:path {:id uuid?}}
               :responses  {200 {:body ::entry}
                            404 {:body {:message string?}}}
               :handler    (fn [{{{:keys [id]} :path} :parameters}]
                             (blog/get-entry id))
               }

      :delete {:summary    "Deletes a blog entry"
               :parameters {:path {:id uuid?}}
               :responses  {204 {:res any?}
                            404 {:body {:message string?}}}
               :handler    (fn [{{{:keys [id]} :path} :parameters}]
                             (blog/delete-entry! id))
               }

      :post   {:summary    "Updates a blog entry"
               :parameters {:path {:id uuid?}
                            :body {:header string? :summary string? :content string?}}
               :responses  {200 {:body {:body ::entry}}
                            404 {:body {:message string?}}}
               :handler    (fn [{{{:keys [id]} :path}                     :parameters
                                 {{:keys [header summary content]} :body} :parameters}]
                             (blog/update-entry! id header summary content))
               }
      }]]])
