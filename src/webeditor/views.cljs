(ns webeditor.views
  (:require
    [re-frame.core :as re-frame]
    [reagent.core :as reagent]
    [sci.core :as sci]
    [webeditor.subs :as subs]))


(def current-item (atom "page1"))


(def rfns (sci/create-ns 're-frame.core nil))
(def rfdbns (sci/create-ns 're-frame.db nil))
(def clojurens (sci/create-ns 'clojure.core nil))


(def re-frame-namespace
  (sci/copy-ns re-frame.core rfns))


(def re-frame-db-namespace
  (sci/copy-ns re-frame.db rfdbns))


(def clojure-namespace
  (sci/copy-ns clojure.core clojurens))


(def namespaces
  {'re-frame.core re-frame-namespace
   're-frame.db re-frame-db-namespace
   'clojure.core clojure-namespace})


(defonce context
  (sci/init {:classes {'js goog/global
                       :allow :all}
             :namespaces {'clojure.core {'println println}
                          're-frame.core re-frame-namespace
                          're-frame.db re-frame-db-namespace}}))


(def currtime (reagent/atom 1))


(js/setInterval (fn []
                  (swap! currtime inc)) 1000)


(def project
  (atom
    ["/page1"]))


(def active
  (atom "/page1"))


(def assets
  ["div"
   "svg"
   "canvas"
   "button"
   "textarea"
   "combobox"
   "image"
   "input"])


(def buttons
  ["new"
   "load"
   "save"
   "test"
   "help"])


(def page-container (reagent/atom [:div {:id "page1"}]))
(def page-code (reagent/atom (str @page-container)))


(def vector_element_code
  (fn [obj env]
    (let [{:keys [x y angle alpha]} obj
          {:keys [mouse_x mouse_y key_down]} env]
      obj)))


(def elements
  {"button" '[:input {:type "button"
                      :value "click"}]
   "div" '[:div {:style {:width "100px"
                         :height "50px"
                         :background-color "#FF44FF"}}]
   "canvas" '[:div
              {:onMouseMove (fn [event]
                              (let [canvas (.-target event)
                                    child (.-firstChild canvas)]
                                (set! (.-left (.-style child)) (str (- (.-clientX event) (.-offsetLeft canvas)) "px"))
                                (set! (.-top (.-style child)) (str (- (.-clientY event) (.-offsetTop canvas)) "px"))))
               :style {:position "relative"
                       :width "800px"
                       :height "600px"
                       :background-color "#FF44FF"}}
              [:div
               {:style {:width "100px"
                        :height "100px"
                        :top "0px"
                        :left "0px"
                        :position "absolute"
                        :background-color "#FF0000"}}]]

   "svg" '[:div
           {:style {:width "100px"
                    :height "100px"
                    :position "relative"
                    ;; :top (str @currtime "px")
                    ;:left (str @currtime "px")
                    :background-color "#FF0000"}}]})


(def elem-counter (atom 0))


(defn text-panel
  []
  (fn []
    [:div {:style {:position "relative"
                   :width "100%"
                   :height "100%"}}
     [:textarea
      {:value @page-code
       :style {:box-sizing "border-box"
               :margin "0px"
               :padding "10px"
               :border-width "0px"
               :width "100%"
               :height "100%"}
       :on-change #(reset! page-code (.. % -target -value))}]
     [:input
      {:type "button"
       :style {:position "absolute"
               :top "0px"
               :right "0px"}
       :value "eval"
       :on-click (fn []
                   (let [comp (sci/eval-string* context @page-code)]
                       ;;(println "COMP" page1)
                     (reset! page-container comp)))}]]))


(defn main-panel
  []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div {:id "main container"
           :style {:display "flex"
                   :flex-direction "column"
                   :width "100%"
                   :height "100%"
                   :margin "0px"
                   :padding "0px"
                   :background-color "#FFEEEE"}}
     [:div {:id "top container"
            :style {:display "flex"
                    :flex "1 1 100%"
                    :height "100%"
                    :overflow "hidden"}}

      [:div {:id "tree container"
             :style {:width "300px"
                     :height "100%"
                     :font-size "14px"
                     :background-color "#EEFFEE"}}
       (for [item @project]
         ^{:key (str item)} [:div
                             {:id (str item)
                              :style {:margin "5px"
                                      :height "30px"
                                      :line-height "30px"
                                      :user-select "none"
                                      :background-color (if (= item @active) "#DEFFDE" "#DEDEDE")}}
                             (str item)])]
      [:div {:style {:display "flex"
                     :width "100%"
                     :flex-direction "column"}}
       [:div {:id "visual editor"
              :onDrop (fn [event]
                        ;; add component skeleton code to the end of the active layers component list
                        (let [type (.getData (. event -dataTransfer) "text")]
                          (let [new-id (str type @elem-counter)
                                new-element (assoc-in (get elements type) [1 :id] new-id)]
                            (swap! elem-counter inc)
                            (swap! page-container conj new-element)
                            (reset! page-code (with-out-str (cljs.pprint/pprint @page-container)))
                            (swap! project conj (str @active "/" new-id)))))
              :onDragOver (fn [event]
                            (.preventDefault event)
                            (set! (.. event -dataTransfer -dropEffect) "move"))
              :style {:width "100%"
                      :height "100%"
                      :background-color "#EEEEFF"}}
        [:div {:id "visual container"}
         (sci/eval-form context @page-container)]]
       [:div {:id "script editor"
              :style {:width "100%"
                      :height "100%"
                      :background-color "#EEFFFF"}}
        [text-panel]]]
      [:div {:style {:width "200px"
                     :height "100%"
                     :flex "1 1 auto"
                     :overflow "auto"}}
       [:div {:id "widget container"
              :style {:width "100%"
                      :display "flex"
                      :box-sizing "border-box"
                      :align-items "center"
                      :flex-direction "column"
                      :background-color "#FFFFEE"}}
        (for [item assets]
          ^{:key (str item)}
          [:div {:id (str item)
                 :draggable "true"
                 :onDragStart (fn [event]
                                (.setData (. event -dataTransfer) "text" item))
                 :style {:background-color "#FF00FF"
                         :border-radius "10px"
                         :margin "10px"
                         :padding "10px"
                         :width "100px"
                         :height "100px"}}
           (str item)])]]]
     [:div {:id "tool container"
            :style {:display "flex"
                    :flex "0 0 30px"
                    :box-sizing "border-box"
                    :width "100%"
                    :background-color "#556655"}}

      (for [item buttons]
        ^{:key (str item)}
        [:div {:id (str item)
               :style {:background-color "#FF00FF"
                       :text-align "center"
                       :margin "5px"
                       :width "100%"
                       :height "20px"}}
         (str item)])]]))
