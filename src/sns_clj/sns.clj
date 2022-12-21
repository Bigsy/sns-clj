(ns sns-clj.sns
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [integrant.core :as ig]
            [clojure.tools.logging :as log])
  (:import [java.io File]
           [java.nio.file Files Paths LinkOption Path]
           [java.nio.file.attribute FileAttribute]))

(def ^:private download-url "https://github.com/s12v/sns/releases/download/0.4.1/sns-0.4.1.jar")

(def ^:private sns-directory (str (System/getProperty "user.home") File/separator ".clj-sns-local"))

(def ^:private host {:name    (System/getProperty "os.name")
                     :version (System/getProperty "os.version")
                     :arch    (System/getProperty "os.arch")})

(defn- ->path
  "Create a path from the given strings."
  [str & strs]
  {:pre [(string? str) (every? string? strs)]}
  (Paths/get str (into-array String strs)))

(defn- path?
  "Is the given argument a path?"
  [x]
  (instance? Path x))

(defn- exists?
  "Does the given path exist?"
  [path]
  {:pre [(path? path)]}
  (Files/exists path (into-array LinkOption [])))

(defn- ensure-sns-directory
  "Make sure the directory that sns Local will be downloaded to
  exists."
  []
  (let [path (->path sns-directory)]
    (when-not (exists? path)
      (-> (Files/createDirectory path (make-array FileAttribute 0))
          (.toString)))))



(defn- build-sns-command
  "Build a java command to start sns Local with the required
  options."
  []
  (let [jar-path (str (io/file sns-directory "sns.jar"))]
    (format "java -jar %s"  jar-path)))


(defn start-sns
  "Start sns Local with the desired options."
  [config]
  (let [sns (.exec (Runtime/getRuntime) (build-sns-command)
                   (into-array String [(str "DB_PATH=" (if config
                                                         config
                                                         (.getPath (clojure.java.io/resource "default_db.json"))))]))]
    (log/info "Started sns Local")
    sns))


(defn- download-sns
  "Download sns."
  [url]
  (log/info "Downloading sns Local" {:sns-directory sns-directory})
  (ensure-sns-directory)
  (io/copy (io/input-stream (io/as-url url)) (io/as-file (str sns-directory "/" "sns.jar"))))

(defn ensure-installed
  "Download and sns Local if it hasn't been already."
  []
  (when-not (exists? (->path sns-directory "sns.jar"))
    (download-sns download-url)))


(defn handle-shutdown
  "Kill the sns Local process on JVM shutdown."
  [sns-process]
  (doto sns-process (.destroy) (.waitFor))
  (log/info (str "Exited" {:exit-value (.exitValue sns-process)})))

(defn create-sns-db-logger
  [log]
  (fn [& message]
    (apply log "snslocal:" message)))

(defn halt! [sns]
  (when sns
    (handle-shutdown sns)))

(defmethod ig/init-key ::sns [_ config]
  (ensure-installed)
  (start-sns config))

(defmethod ig/halt-key! ::sns [_ sns]
  (halt! sns))