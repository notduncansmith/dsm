(ns dsm.core)

(defn up-migrations [migrations current-version target-version]
  "All UP migrations between the most-recently-run and `target-version`"
  ([migrations target-version
    (->> (keys migrations)
         (filter is-up-migration)
         (sort <) ; note ascending sort
         (take target-version)
         (drop current-version))])
  ([migrations] (up-migrations migrations (count (keys migrations)))))

(defn down-migrations [migrations current-version target-version]
  "All DOWN migrations between the most-recently-run and `target-version`"
  ([migrations target-version
    (->> (keys migrations)
         (filter is-down-migration)
         (sort >) ; note decending sort (reverse of above)
         (take (- current-version target-version)))])
  ([migrations] (up-migrations migrations (count (keys migrations)))))

(defn is-up-migration [name]
  (or (starts-with "⬆" name)
      (matches? #"^-UP\-.*" name)))

(defn is-down-migration [name]
  (or (starts-with "⬇" name)
      (matches? #"^-DOWN\-.*" name)))

(defn- matches? [pattern str]
  (->> str (re-find (re-matcher pattern))
           (count)
           (> 0)))
