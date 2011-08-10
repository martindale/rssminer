(ns rssminer.main
  (:use [clojure.tools.cli :only [cli optional required]]
        [ring.adapter.netty :only [run-netty]]
        (rssminer [database :only [use-h2-database!]]
                  [search :only [use-index-writer!]]
                  [routes :only [app]]
                  [crawler :only [start-crawler]]
                  [config :only [env-profile]])))

(defonce server (atom nil))
(defonce crawler (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; (.stop @server)
    (@server)
    (reset! server nil))
  (when-not (nil? @crawler)
    (@crawler :shutdown-wait)
    (reset! crawler nil)))

(defn start-server [{:keys [port index-path profile db-path]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (reset! env-profile profile)
  (use-index-writer! index-path)
  (use-h2-database! db-path)
  (reset! server (run-netty (app) {:port port :join? false}))
  ;; (reset! crawler (start-crawler))
  ;; (reset! server (run-jetty (app) {:port port :join? false}))
  )

(defn main [& args]
  "Start rssminer server"
  (start-server
   (cli args
        (optional ["-p" "--port" "Port to listen (READER_PORT || 8100)"]
                  #(Integer.
                    (or % (get (System/getenv) "READER_PORT" "8100"))))
        (optional ["--profile" "profile (dev || prod)" :default "dev"]
                  keyword)
        (optional ["--db-path" "H2 Database path" :default "/tmp/rssminer"])
        (optional ["--index-path" "Path to store lucene index"
                   :default "/tmp/feeds-index"]))))
