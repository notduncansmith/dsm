(ns dsm.core)

(defn- starts-with? [substr str] (= substr (subs str 0 (.length substr))))

(defn is-up-migration [mname] (starts-with? "-UP-" mname))
(defn is-down-migration [mname] (starts-with? "-DOWN-" mname))

(defn up-migrations
  "All UP migrations between `current-version` and `target-version`"
  ([migrations current-version target-version]
   (->> (keys migrations)
        (map name)
        (filter is-up-migration)
        (sort)
        (take target-version)
        (drop current-version)
        (vec)))
  ([migrations current-version]
   (up-migrations migrations current-version (count (keys migrations)))))

(defn down-migrations
  "All DOWN migrations between `current-version` and `target-version`"
  ([migrations current-version target-version]
   (->> (keys migrations)
        (map name)
        (filter is-down-migration)
        (sort)
        (take current-version)
        (drop target-version)
        (reverse)
        (vec)))
  ([migrations current-version]
   (down-migrations migrations current-version 0)))
