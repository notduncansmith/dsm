(ns dsm.core-test
  (:require [clojure.test :refer :all]
            [dsm.core :refer :all]
            [hugsql.core :refer [map-of-db-fns-from-string]]))

(def migrations (map-of-db-fns-from-string "
-- :name -UP-m1-wal-pragma :*
pragma journal_mode = WAL;

-- :name -DOWN-m1-wal-pragma :?
select 1; -- does nothing, this pragma is permanent

-- :name -UP-m2-foreign-key-pragma :!
pragma foreign_keys = on;

-- :name -DOWN-m2-foreign-key-pragma :?
select 1; -- does nothing, this pragma is permanent

-- :name -UP-m3-create-users-table :!
create table if not exists \"users\" (
  \"id\" integer primary key autoincrement not null,
  \"created_at\" integer not null,
  \"username\" char(128) not null,
  \"access_token\" char(128) not null
);

-- :name -DOWN-m3-drop-users-table :!
drop table if exists \"users\";

-- :name -UP-m4-create-unique-username-index :!
create unique index if not exists user_usernames on \"users\" (\"username\");

-- :name -DOWN-m4-drop-unique-username-index :!
drop index if exists user_usernames;
"))

(deftest test-up-migrations
  (testing "Up migrations begin with `-UP-`"
    (is (true? (= [ "-UP-m1-wal-pragma"
                    "-UP-m2-foreign-key-pragma"
                    "-UP-m3-create-users-table"
                    "-UP-m4-create-unique-username-index"]
                  (up-migrations migrations 0 4)))))

  (testing "Only migrations in range are included"
    (is (true? (= [ "-UP-m2-foreign-key-pragma"
                    "-UP-m3-create-users-table"
                    "-UP-m4-create-unique-username-index"]
                  (up-migrations migrations 1 4))))))

(deftest test-down-migrations
  (testing "Down migrations begin with `-DOWN-`"
    (is (true? (= [ "-DOWN-m4-drop-unique-username-index"
                    "-DOWN-m3-drop-users-table"
                    "-DOWN-m2-foreign-key-pragma"
                    "-DOWN-m1-wal-pragma"]
                  (down-migrations migrations 4 0)))))

  (testing "Only migrations in range are included"
    (is (true? (= ["-DOWN-m4-drop-unique-username-index"]
                  (down-migrations migrations 4 3))))))
