(ns blogger.components.common)

(defn input [type id placeholder fields]
  [:input
   {:type type
    :placeholder placeholder
    :value (id @fields)
    :on-change #(swap! fields assoc id (-> % .-target .-value))}])

(defn form-input [type label id placeholder fields]
  [:div
   [:label label]
   [:div [input type id placeholder fields]]])

(defn text-input [label id placeholder fields]
  (form-input :text label id placeholder fields))

(defn password-input [label id placeholder fields]
  (form-input :password label id placeholder fields))

(defn textarea-input [label id placeholder fields]
  [:div
   [:label {:for id} label]
   [:div
    [:textarea
     {:id id
      :value (id @fields)
      :placeholder placeholder
      :cols 30
      :rows 30
      :on-change #(swap! fields assoc id (-> % .-target .-value))}]]])

(defn request-error [error]
  (fn []
    (when-let [message (:message @error)]
      [:div.alert.alert-danger (str message " - Check network-tab for details.")])))

(defn request-success [response]
  (fn []
    (when-let [message (:message @response)]
      [:div.alert.alert-success (str message " - Check network-tab for details.")])))

(defn danger-button [handler text]
  [:button.btn.btn-danger
   {:on-click handler}
   text])

(defn primary-button [handler text]
  [:button.btn.btn-primary
   {:on-click handler}
   text])