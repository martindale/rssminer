--rlwrap java -cp /tmp/h2-1.3.158.jar org.h2.tools.Shell -url jdbc:h2:/tmp/test/crawler_tmph92 -user sa -password sa

-- select datediff('SECOND', add_ts, now()) - check_interval as t from rss_link order by t limit 1

SET COMPRESS_LOB  DEFLATE;
----
CREATE TABLE users
(
  id serial PRIMARY KEY,
  email VARCHAR UNIQUE,
  name VARCHAR,
  password VARCHAR,
  authen_toekn VARCHAR,
  added_ts timestamp DEFAULT now()
);

----
create table crawler_links (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  title VARCHAR,
  added_ts TIMESTAMP default now(),
  domain VARCHAR,         --assume one domain, one rss, do not crawler
  next_check_ts INTEGER default 1,
  last_modified VARCHAR,
  last_md5 VARCHAR,
  check_interval INTEGER default 60 * 60 * 24 * 10, -- in seconds, ten days
  server VARCHAR,
  referer_id INTEGER REFERENCES crawler_links
      ON UPDATE CASCADE ON DELETE SET NULL,
)

----
create table rss_links (
  id INTEGER PRIMARY KEY auto_increment,
  url VARCHAR UNIQUE,
  title VARCHAR,
  description VARCHAR,
  alternate VARCHAR,            -- usually, the site's link
  added_ts TIMESTAMP default now(),
  next_check_ts INTEGER default 1,
  check_interval INTEGER default 60 * 60 * 24, -- in seconds, one day
  last_modified VARCHAR,                -- from http response header
  last_md5 VARCHAR,                     -- used to check if changed
  favicon VARCHAR,             -- base64 encoded, TODO, change to cblob
  server VARCHAR,              -- from http response header
  subscription_count INTEGER default 0, -- how much user subscribed
  user_id INTEGER REFERENCES users      -- who first add it
     ON UPDATE CASCADE ON DELETE SET NULL,
  crawler_link_id INTEGER  REFERENCES crawler_links
     ON UPDATE CASCADE ON DELETE SET NULL,
)
----
create index idx_cl_domain on crawler_links(domain)
----
create index idx_cl_next_check_ts on crawler_links(next_check_ts)
----
create index idx_rl_next_check_ts on rss_links(next_check_ts)
----
CREATE TABLE user_subscriptions
(
  id INTEGER PRIMARY KEY auto_increment,
  user_id INTEGER NOT NULL
       REFERENCES users  ON UPDATE CASCADE ON DELETE CASCADE,
  rss_link_id INTEGER NOT NULL
       REFERENCES rss_links  ON UPDATE CASCADE ON DELETE CASCADE,
  title VARCHAR, --user defined title, default is subscription's title
  group_name VARCHAR default 'ungrouped',
  added_ts TIMESTAMP DEFAULT now(),
  UNIQUE (user_id, rss_link_id)
);

----
create table multi_rss_domains (
  domain varchar UNIQUE,
)
----
create table rss_xmls (
  id INTEGER PRIMARY KEY auto_increment,
  added_ts TIMESTAMP default now(),
  last_modified TIMESTAMP,
  length INTEGER,
  content CLOB,
  rss_link_id INTEGER REFERENCES rss_links
    ON UPDATE CASCADE ON DELETE SET NULL
)
----
CREATE TABLE feeds
(
  id INTEGER PRIMARY KEY auto_increment,
  author VARCHAR,
  title VARCHAR,
  summary VARCHAR,              -- TODO change bo clob
  alternate VARCHAR,
  updated_ts TIMESTAMP,
  published_ts TIMESTAMP,
  rss_link_id INTEGER
             REFERENCES rss_links ON UPDATE CASCADE ON DELETE CASCADE,
  rss_xml_id INTEGER
             REFERENCES rss_xmls ON UPDATE CASCADE ON DELETE CASCADE,
);
----
CREATE TABLE comments
(
  id INTEGER PRIMARY KEY auto_increment,
  content VARCHAR,
  user_id INTEGER NOT NULL
          REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
  feed_id INTEGER NOT NULL
          REFERENCES feeds  ON UPDATE CASCADE ON DELETE CASCADE,
  added_ts TIMESTAMP  DEFAULT now(),
);
---
CREATE TABLE feedcategory
(
    id INTEGER PRIMARY KEY auto_increment,
    type VARCHAR, -- possible val: tag, freader(system type),
    text VARCHAR, -- freader-> stared, read
    user_id INTEGER NOT NULL
            REFERENCES users ON UPDATE CASCADE ON DELETE CASCADE,
    feed_id INTEGER NOT NULL
            REFERENCES feeds ON UPDATE CASCADE ON DELETE CASCADE,
    added_ts TIMESTAMP DEFAULT now(),
    UNIQUE(type, text, user_id, feed_id)
);

----
insert into crawler_links (url, domain) values --seeds
('http://blog.jquery.com/', 'http://blog.jquery.com'),
('http://blogs.oracle.com/', 'http://blogs.oracle.com'),
('http://blog.sina.com.cn/', 'http://blog.sina.com.cn'),
('http://blog.sina.com.cn/kaifulee', 'http://blog.sina.com.cn'),
('http://briancarper.net/', 'http://briancarper.net'),
('http://channel9.msdn.com/', 'http://channel9.msdn.com'),
('http://clj-me.cgrand.net/', 'http://clj-me.cgrand.net'),
('http://data-sorcery.org/', 'http://data-sorcery.org'),
('http://ejohn.org/', 'http://ejohn.org'),
('http://emacs-fu.blogspot.com/', 'http://emacs-fu.blogspot.com'),
('http://emacsblog.org/', 'http://emacsblog.org'),
('http://norvig.com', 'http://norvig.com'),
('http://planet.clojure.in/', 'http://planet.clojure.in'),
('http://testdrivenwebsites.com/', 'http://testdrivenwebsites.com'),
('http://timepedia.blogspot.com/', 'http://timepedia.blogspot.com'),
('http://weblogs.asp.net/scottgu/', 'http://weblogs.asp.net'),
('http://www.masteringemacs.org/', 'http://www.masteringemacs.org'),
('http://www.ruanyifeng.com/blog/', 'http://www.ruanyifeng.com'),
('http://www.ubuntugeek.com/', 'http://www.ubuntugeek.com'),
('https://www.ibm.com/developerworks/', 'https://www.ibm.com'),
('http://www.omgubuntu.co.uk/', 'http://www.omgubuntu.co.uk'),
('http://tech2ipo.com/', 'http://tech2ipo.com'),
('http://www.dbanotes.net/', 'http://www.dbanotes.net'),
('http://xianguo.com/hot', 'http://xianguo.com')

----
insert into rss_links (url) values
('http://feeds.feedburner.com/ruanyifeng'),
('http://blog.sina.com.cn/rss/kaifulee.xml'),
('http://cemerick.com/feed/'),

----
insert into multi_rss_domains (domain) values
('http://blog.sina.com.cn'),
('http://blogs.oracle.com'),
('http://xianguo.com'),
('http://www.ibm.com')