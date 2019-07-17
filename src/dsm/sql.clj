(ns dsm.sql
  (:require [hugsql.core :refer [def-db-fns-from-string]]
            [dsm.core :refer [up-migrations down-migrations is-up-migration is-down-migration]]))

(def default-conn
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "./data.db"})

(def-db-fns-from-string "
-- :name create-migrations-table :!
create table if not exists \"migrations\" (
  \"id\" integer primary key autoincrement not null,
  \"name\" char(128) not null
);

-- :name count-saved-migrations :1
select count(1) as count from migrations;

-- :name record-migration :!
insert into migrations (name) values (:name);

-- :name delete-migrations-down-to :!
delete from migrations where id > :id;
")

(defn migration-version
  "Get the number of migrations that have been run"
  [conn]
  (-> (count-saved-migrations conn) (first) (:count)))

(defn run-migrations
  "Run the given migrations, print their names, and record them"
  [conn migrations names]
  (mapv
    #(do (println %)
        ((get-in migrations [(keyword %) :fn]) conn)
        (if (is-up-migration %)
            (record-migration conn {:name %})
            (delete-migrations-down-to conn
              {:id (.indexOf (->> migrations
                              (keys)
                              (sort)
                              (map name)
                              (filter is-down-migration)) %)})))
    names))

(defn migrate-up
  "Run all UP migrations between the most-recently-run and `target-version`"
  ([conn migrations target-version]
   (let [current-version (migration-version conn)
         migrations-to-run (up-migrations migrations current-version target-version)]
     (run-migrations conn migrations migrations-to-run)))
  ([conn migrations] (migrate-up conn migrations (count (keys migrations)))))

(defn migrate-down
  "Run all DOWN migrations between the most-recently-run and `target-version`"
  ([conn migrations target-version]
   (let [current-version (migration-version conn)
         migrations-to-run (down-migrations migrations current-version target-version)]
     (run-migrations conn migrations migrations-to-run)))
  ([conn migrations] (migrate-down conn migrations 0)))
