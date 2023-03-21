(ns dk.cst.regionh.scrape
  "Scrape JSON data from a list of Region Hovestaden URLs (with permission)."
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]))

(def regionh-urls
  (delay (line-seq (io/reader "regionh-urls.txt"))))

(defn url->dbpath
  [url]
  (second (re-find #"dbpath=([^&]+)&" url)))

(defn url->unid
  [url]
  (second (re-find #"unid=([^&]+)&" url)))

(defn api-url
  "Build the actual URL used to retrieve document data from the `url`."
  [url]
  (str "https://vip.regionh.dk" (url->dbpath url)
       "/aLoadInfoDokRead?OpenAgent&id=" (url->unid url)))

(defn persist-results!
  "Write each of the in-memory `results` to disk at 'out/results/<unid>.json'."
  [results]
  (io/make-parents (str "out/results/*"))
  (let [failed (atom 0)]
    (doseq [result results]
      (let [{:keys [data success]} (json/read-str result :key-fn keyword)]
        (if success
          (spit (str "out/results/" (:id data) ".json") result)
          (swap! failed inc))))
    (println (str (- (count results) @failed) " succeeded, "
                  @failed " failed\n"))))

(comment
  ;; Fetch every document as JSON via the regionh API
  (def docs
    (pmap (comp slurp api-url) @regionh-urls))

  ;; Analyse failures.
  (filter (fn [result]
            (false? (:success (json/read-str result :key-fn keyword))))
          docs)

  ;; Easier view of the metadata
  (dissoc (:data (json/read-str (first docs) :key-fn keyword))
          :BodyWeb :ReleaseLog :LevelDisplay :DSPLevelDisplayMakeValid)

  ;; Persist all of the downloaded docs to disk
  (persist-results! docs)
  #_.)
