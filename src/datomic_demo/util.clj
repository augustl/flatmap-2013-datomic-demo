(ns datomic-demo.util
  (:require [datomic.api :as d]))

(defn get-inserted-entity
  [tx tempid]
  (d/touch (d/entity (:db-after tx) (d/resolve-tempid (:db-after tx) (:tempids tx) tempid))))

(defn insert
  [datomic-conn facts]
  (let [tempid (d/tempid :db.part/user)
        tx @(d/transact
             datomic-conn
             (map #(concat [:db/add tempid] %) facts))]
    (get-inserted-entity tx tempid)))