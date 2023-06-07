(ns main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(defonce current-time (r/atom 5))
(defonce workout-time (r/atom 5))
(defonce cooldown-time (r/atom 8))
(defonce timer-state (r/atom :stopped))
(defonce page (r/atom {:page 1
                       :text "temp"}))

(def start-sound (js/Audio. "sounds/start.wav"))
(def stop-sound (js/Audio. "sounds/stop.wav"))

(defn play-sound [sound]
  (-> sound .play))

(defn get-page [page-number]
  (go (let [response (<! (http/get (str "./workouts/" page-number ".txt")))]
        (when (:success response)
          (reset! page {:page page-number
                        :text (:body response)})))))

(defn next-page []
  (let [current-page (:page @page)]
    (get-page (inc current-page))))

(defn prev-page []
  (let [current-page (:page @page)]
    (get-page (dec current-page))))

(defonce time-tick
  (js/setInterval
   (fn []
     (let [time @current-time
           state @timer-state]
       (when (not= state :stopped)
         (cond (and (= state :workout)
                    (< time 1))
               (do
                 (reset! timer-state :cooldown)
                 (reset! current-time @cooldown-time)
                 (play-sound stop-sound))
               (and (= state :cooldown)
                    (< time 1))
               (do
                 (reset! timer-state :workout)
                 (reset! current-time @workout-time)
                 (play-sound start-sound)))
         (swap! current-time dec)))) 1000))

(defn handle-timer-click []
  (case @timer-state
    :stopped (do (reset! timer-state :workout)
                 (reset! current-time @workout-time)
                 (play-sound start-sound))
    (reset! timer-state :stopped)))

(defn timer []
  [:div.timer-container
   [:button.timer
    {:on-click handle-timer-click}
    (if (= @timer-state :stopped)
      @workout-time
      @current-time)]])

(defn simple-example []
  [:main
   [:label
    "Workout "
    [:input {:type "number"
             :value @workout-time
             :on-change #(reset! workout-time (-> % .-target .-value))}]]
   [:label
    "Cooldown "
    [:input {:type "number"
             :value @cooldown-time
             :on-change #(reset! cooldown-time (-> % .-target .-value))}]]
   (timer)
   [:div
    [:h1 (str "Workout #" (:page @page))]
    [:pre.workout-text (:text @page)]]
   [:div
    [:button {:on-click prev-page} "Prev"]
    [:button {:on-click next-page} "Next"]]])

(defn ^export init []
  (get-page 1)
  (rdom/render [simple-example] (js/document.getElementById "app")))