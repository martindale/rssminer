(ns rssminer.db.user-feed
  (:use [rssminer.db.util :only [mysql-query with-mysql]]
        [rssminer.util :only [now-seconds]]
        [clojure.java.jdbc :only [do-commands]]))

;;; vote time is not recoreded
(defn insert-user-vote [user-id feed-id vote]
  (let [n (now-seconds)]
    (with-mysql (do-commands ;; rss_link_id default 0, which is ok
                 (format "INSERT INTO user_feed
                      (user_id, feed_id, vote_user, vote_date)
             VALUES (%d, %d, %d, %d) ON DUPLICATE KEY
             UPDATE vote_user = %d, vote_date = %d"
                         user-id feed-id vote n vote n)))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)]
    (with-mysql (do-commands ;; rss_link_id default 0
                 (format "INSERT INTO user_feed (user_id, feed_id, read_date)
       VALUES (%d, %d, %d) ON DUPLICATE KEY UPDATE read_date = %d"
                         user-id feed-id now now)))))

;;; this is not right, if user_feed has no entry, then it's not
;;; fecthed
;;; old 2012/5/23
(defn fetch-newest-1 [user-id limit offset]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link,
         f.rss_link_id, f.published_ts, uf.vote_user, uf.vote_sys
          FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ?
          ORDER BY f.published_ts DESC LIMIT ? OFFSET ?"
                user-id limit offset]))

(defn fetch-newest [user-id limit offset]
  (mysql-query ["SELECT id, title, author, link, rss_link_id,
           published_ts from feeds WHERE rss_link_id IN
        (SELECT rss_link_id FROM user_subscription WHERE user_id = ?)
        ORDER BY published_ts DESC LIMIT ? OFFSET ?" user-id limit offset]))

;;; fetch unread, sort by vote_sys desc
(defn fetch-system-voteup [user-id limit offset]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link,
         f.rss_link_id, f.published_ts, uf.vote_sys
         FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? AND uf.read_date = -1
              ORDER BY uf.vote_sys DESC LIMIT ? OFFSET ?"
                user-id limit offset]))

;;; fetch readed, sort by read_date desc
(defn fetch-recent-read [user-id limit offset]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link, f.rss_link_id,
         f.published_ts, uf.vote_user, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? AND uf.read_date > 0
              ORDER BY uf.read_date DESC LIMIT ? OFFSET ?"
                user-id limit offset]))

;;; fetch voted
(defn fetch-recent-voted [user-id limit offset]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link,
     f.rss_link_id, f.published_ts, uf.vote_user, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? AND uf.vote_user != 0 LIMIT ? OFFSET ?"
                user-id limit offset]))
