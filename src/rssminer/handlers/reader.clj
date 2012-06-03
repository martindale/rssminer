(ns rssminer.handlers.reader
  (:use (rssminer [util :only [user-id-from-session to-int]]
                  [search :only [search* search-within-subs]])
        [rssminer.db.user :only [find-user-by-id]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg])
  (:import rssminer.Utils))

(defn landing-page [req]
  (view/landing-page))

(defn app-page [req]
  (let [uid (user-id-from-session req)]
    (view/app-page {:rm {:user (find-user-by-id uid)
                         :no_iframe Utils/NO_IFRAME
                         :reseted Utils/RESETED_DOMAINS
                         :static_server (:static-server @cfg/rssminer-conf)
                         :proxy_server (:proxy-server @cfg/rssminer-conf)}})))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [q limit ids]} (:params req)
        user-id (user-id-from-session req)
        limit (min 20 (to-int limit))]
    (if ids
      (search-within-subs q (clojure.string/split ids #",") limit)
      (search* q (user-id-from-session req) limit))))
