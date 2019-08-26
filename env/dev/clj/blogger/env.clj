(ns blogger.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [blogger.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[blogger started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[blogger has shut down successfully]=-"))
   :middleware wrap-dev})
