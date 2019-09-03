(ns blogger.components.session
  (:require [reagent.core :refer [atom]]
            [alandipert.storage-atom :refer [local-storage clear-local-storage!]]))

(def session (local-storage (atom {}) :session))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))