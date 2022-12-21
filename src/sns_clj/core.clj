(ns sns-clj.core
  (:require [clojure.pprint :as pprint]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [org.httpkit.client :as http]
            [sns-clj.state :as state]))

(def default-config
  nil)

(defn ->ig-config [config-path]
  {:sns-clj.sns/sns config-path})

(defn halt-sns! []
  (when @state/state
    (swap! state/state
           (fn [s]
             (ig/halt! (:system s))
             nil))))

(defmacro retry
  [cnt expr]
  (letfn [(go [cnt]
            (if (zero? cnt)
              expr
              `(try ~expr
                    (catch Exception e#
                      (retry ~(dec cnt) ~expr)))))]
    (go cnt)))

(defn init-sns
  ([] (init-sns default-config))
  ([config]
   (let [ig-config (->ig-config config)
         config-pp (with-out-str (pprint/pprint config))]
     (log/info "starting sns with config:" config-pp)
     (try
       (halt-sns!)
       (ig/load-namespaces ig-config)
       (reset! state/state
               {:system (ig/init ig-config)
                :config ig-config})
       (retry 30 (when (:error @(http/get (format "http://localhost:%s/" "9911")))
                   (do (Thread/sleep 200) (throw (Exception.)))))
       (catch clojure.lang.ExceptionInfo ex
         (ig/halt! (:system (ex-data ex)))
         (throw (.getCause ex)))))))

(defn with-sns-fn
  "Startup with the specified configuration; executes the function then shuts down."
  ([config f]
   (try
     (init-sns config)
     (f)
     (finally
       (halt-sns!))))
  ([f]
   (with-sns-fn default-config f)))

(defmacro with-sns
  "Startup with the specified configuration; executes the body then shuts down."
  [config & body]
  `(with-sns-fn ~config (fn [] ~@body)))

(comment (init-sns)
         (halt-sns!))