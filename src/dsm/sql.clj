(ns dsm.sql
  (:require [hugsql.core :refer [def-db-fns-from-string]]))

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
")

(defn migration-version
  "Get the current migration number (which is also the number of migrations that have been run)"
  [conn]
  (-> (count-saved-migrations conn) (first) (:count)))

(defn migrate-up
  "Run all UP migrations between the most-recently-run and `target-version`"
  ([conn migrations target-version
    (let [current-version (migration-version conn)
          remaining-migrations (->> (keys migrations)
                                    (filter is-up-migration)
                                    (sort <) ; note ascending sort
                                    (take target-version)
                                    (drop current-version))]
      (run-migrations remaining-migrations))])
  ([conn migrations] (migrate-up conn migrations (count (keys migrations)))))

(defn migrate-down
  "Run all DOWN migrations between the most-recently-run and `target-version`"
  ([conn migrations target-version]
   (let [current-version (migration-version conn)
         remaining-migrations (->> (keys migrations)
                                   (filter is-down-migration)
                                   (sort >) ; note decending sort (reverse of above)
                                   (take (- current-version target-version)))]
      (run-migrations remaining-migrations)))
  ([conn migrations] (migrate-down 0)))

(defn run-migrations
  "Run the given migrations, print their names, and record them"
  [conn migrations names]
  (mapv #(do (println %)
             ((get-in migrations [% :fn]) conn)
             (record-migration conn {:name %}))
        names))
