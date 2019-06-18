# dsm
[![Build Status](https://travis-ci.org/notduncansmith/dsm.svg?branch=master)](https://travis-ci.org/notduncansmith/dsm)
[![codecov](https://codecov.io/gh/notduncansmith/dsm/branch/master/graph/badge.svg)](https://codecov.io/gh/notduncansmith/dsm)
[![Clojars Project](https://img.shields.io/clojars/v/dsm.svg)](https://clojars.org/dsm)

dsm (short for "dead simple migrations") is a small Clojure library designed to run migration functions.

Migrations are given as a map of names to functions. "Up" migration names start with `-UP-`, and "down" migration names start with `-DOWN-`.

`dsm.core` contains the logic for deciding which migrations to run, and `dsm.sql` contains utilities for running those against a JDBC connection spec (as well as maintaining a migrations table).

`dsm.sql` depends on a working SQL database connection, which is used to record migration progress and is given as the first argument to all migration functions.

```clj
[dsm "0.1.0"]
```

## Usage

```clj
(ns my-awesome-app.core
    (:require [dsm.sql :refer [create-migrations-table migrate-up]]
              [hugsql.core :refer [map-of-db-fns-from-string]]))

(def conn {:classname "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname (or (System/getenv "AWESOME_DB_PATH")
                        "./resources/awesome.db")})

; First, using the magic of HugSQL, define migrations
(def migrations (map-of-db-fns-from-string "
-- :name -UP-m1-wal-pragma :*
pragma journal_mode = WAL;

-- :name -UP-m2-foreign-key-pragma :!
pragma foreign_keys = on;

-- :name -UP-m3-create-users-table :!
create table if not exists \"users\" (
  \"id\" integer primary key autoincrement not null,
  \"created_at\" integer not null,
  \"username\" char(128) not null,
  \"access_token\" char(128) not null
);

-- :name -DOWN-m3-drop-users-table :!
drop table \"users\";

-- :name -UP-m4-create-unique-username-index :!
create unique index if not exists user_usernames on \"users\" (\"username\");

-- :name -DOWN-m4-drop-unique-username-index :!
drop index user_usernames;
")

; Next, we create a place to store our migration state
(create-migrations-table conn)

; Finally, run the migrations!
(migrate-up conn migrations 4)
```

## TODO

[ ] Write core tests
[ ] Publish to Clojars

## License

Copyright © 2018 Duncan Smith

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.