;; # Introducing Clerk 👋
(ns nextjournal.clerk
  (:require [clojure.string :as str]
            [datoteka.core :as fs]
            [nextjournal.beholder :as beholder]
            [nextjournal.clerk.hashing :as hashing]
            [nextjournal.clerk.webserver :as webserver]))


(defn read+eval-cached [vars->hash code-string]
  (let [cache-dir (str fs/*cwd* fs/*sep* ".cache")
        form (read-string code-string)
        hash (hashing/hash vars->hash (hashing/analyze form))
        cache-file (str cache-dir fs/*sep* hash)
        no-cache? (hashing/no-cache? form)]
    #_(prn :hash hash :eval form :cached? (boolean (and (not no-cache?) (fs/exists? cache-file))))
    (fs/create-dir cache-dir)
    (if (and (not no-cache?) (fs/exists? cache-file))
      (read-string (slurp cache-file))
      (let [result (eval form)
            var-value (cond-> result (var? result) deref)]
        (if (fn? var-value)
          result
          (do (when-not (or no-cache?
                            (instance? clojure.lang.IDeref var-value)
                            (instance? clojure.lang.MultiFn var-value)
                            (contains? #{'ns 'in-ns 'require} (first form)))
                (spit cache-file (binding [*print-meta* true] (pr-str var-value))))
              var-value))))))

(defn clear-cache!
  ([]
   (let [cache-dir (str fs/*cwd* fs/*sep* ".cache")]
     (when (fs/exists? cache-dir)
       (fs/delete (str fs/*cwd* fs/*sep* ".cache"))))))


(defn +eval-results [vars->hash doc]
  (into []
        (map (fn [{:as cell :keys [type text]}]
               (cond-> cell
                 (= :code type)
                 (assoc :result (read+eval-cached vars->hash text)))))
        doc))

#_(+eval-results {} [{:type :markdown :text "# Hi"} {:type :code :text "(+ 39 3)"}])

(defn parse-file [file]
  (hashing/parse-file {:markdown? true} file))

#_(parse-file "notebooks/elements.clj")

(defn eval-file [file]
  (->> file
       parse-file
       (+eval-results (hashing/hash file))))

#_(eval-file "notebooks/elements.clj")

(defn show!
  "Converts the Clojure source test in file to a series of text or syntax panes and causes `panel` to contain them."
  [file]
  (let [doc (parse-file file)]
    ;; TODO diff to avoid flickering
    #_(webserver/update-doc! doc)
    (webserver/update-doc! (+eval-results (hashing/hash file) doc))))

(defn file-event [{:keys [type path]}]
  (when-let [ns-part (and (= type :modify)
                          (second (re-find #".*/src/(.*)\.clj" (str path))))]
    (binding [*ns* (find-ns (symbol (str/replace ns-part fs/*sep* ".")))]
      (nextjournal.clerk/show! (str path)))))

;; And, as is the culture of our people, a commend block containing
;; pieces of code with which to pilot the system during development.
(comment
  (def watcher
    (beholder/watch #(file-event %) "src"))

  (beholder/stop watcher)

  (show! "notebooks/elements.clj")
  (show! "src/nextjournal/clerk/hashing.clj")
  (show! "src/nextjournal/clerk/core.clj")

  ;; Clear cache
  (clear-cache!)

  )
