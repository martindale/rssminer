(ns rssminer.handlers.users
  (:use  [ring.util.response :only [redirect]]
         (rssminer [time :only [now-seconds]]
                   [util :only [session-get assoc-if]]
                   [config :only [rssminer-conf]])
         [ring.middleware.file-info :only [make-http-format]]
         [clojure.data.json :only [json-str read-json]])
  (:require [rssminer.db.user :as db]
            [rssminer.db.user-feed :as uf]
            [rssminer.views.users :as view])
  (:import [java.util Locale Calendar TimeZone Date]
           java.text.SimpleDateFormat))

(defn- get-expire "get string for http expire header" [days]
  (let [^SimpleDateFormat f (make-http-format)
        c (doto (Calendar/getInstance)
            (.add Calendar/DAY_OF_YEAR days))
        d (.getTime c)]
    (.format f d)))

(defn show-login-page [req]
  (view/login-page "/a"))

(defn login [req]
  (let [{:keys [email password return-url persistent]} (:params req)
        user (db/authenticate email password)
        return-url (or return-url "/a")]
    (if user
      (assoc (redirect return-url)
        :session {:user (select-keys user [:id :email :name :conf])}
        :session-cookie-attrs (if persistent
                                {:expires (get-expire 7)
                                 :http-only true}
                                {:http-only true}))
      (view/login-page return-url))))

(defn show-signup-page [req]
  (view/signup-page))

(defn signup [req]
  (let [{:keys [email password]} (:params req)
        user (db/create-user {:email email
                              :added_ts (now-seconds)
                              :password password})]
    (assoc (redirect "/a")              ; no conf currently
      :session {:user (select-keys user [:id :email :name])})))

;;; :nav => show and hide of left nav
;;; :height => bottom feed list height
;;; :width => nav width
;;; :expire => feed mark as read after X days
;;; :like_threshhold => more than it mean like
;;; :dislike_threshhold => less than it mean dislike
(defn save-pref [req]
  (let [user (session-get req :user)
        updated (merge
                 (:conf user)
                 (select-keys (:body req) [:nav :height :width :expire]))]
    (db/update-user (:id user) {:conf (json-str updated)})
    {:status 204
     :body nil
     :session {:user (assoc user :conf updated)}}))

(defn welcome-list [req]
  (let [u-id (:id (session-get req :user))]
    {:read (uf/fetch-recent-read u-id 30)
     :voted (uf/fetch-recent-voted u-id 20)
     :recommend (uf/fetch-system-voteup u-id 20)}))

(defn google-openid [req]
  (let [spec "http://specs.openid.net/auth/2.0/identifier_select"
        host (if (= (@rssminer-conf :profile) :dev)
               "localhost:9090/" "rssminer.net/")
        url (str "https://www.google.com/accounts/o8/ud"
                 "?openid.ns=http://specs.openid.net/auth/2.0"
                 "&openid.ns.pape=http://specs.openid.net/extensions/pape/1.0"
                 "&openid.ns.max_auth_age=300"
                 "&openid.claimed_id=" spec
                 "&openid.identity=" spec
                 "&openid.mode=checkid_setup"
                 "&openid.ui.ns=http://specs.openid.net/extensions/ui/1.0"
                 "&openid.ui.mode=popup"
                 "&openid.ui.icon=true"
                 "&openid.ns.ax=http://openid.net/srv/ax/1.0"
                 "&openid.ax.mode=fetch_request"
                 "&openid.ax.type.email=http://axschema.org/contact/email"
                 "&openid.ax.required=email"
                 "&openid.return_to=http://" (str host "login/checkauth")
                 (str "&openid.realm=http://" host))]
    (redirect url)))

(defn checkauth [req]
  (if-let [email ((:params req) "openid.ext1.value.email")]
    (assoc (redirect "/a")
      :session {:user (select-keys
                       (or (db/find-user {:email email})
                           (db/create-user {:email email
                                            :added_ts (now-seconds)
                                            :provider "google"}))
                       [:id :email :name :conf])})
    (redirect "/")))
