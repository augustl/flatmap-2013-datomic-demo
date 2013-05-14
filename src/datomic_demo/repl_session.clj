(do
  (require '[datomic.api :as d])
  (require '[datomic-demo.util :as util])
  (import 'java.util.Date)
  (def datomic-url "datomic:mem://flatmap-demo")
  (d/create-database datomic-url)
  (def datomic-conn (d/connect datomic-url)))



(deref
 (d/transact
  datomic-conn
  [{:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :user/password-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}]))



(def user-tempid (d/tempid :db.part/user))
(def insert-tx
  (deref
   (d/transact
    datomic-conn
    [[:db/add user-tempid :user/email "august@augustl.com"]
     [:db/add user-tempid :user/password-hash "123abc"]])))



(def our-db (d/db datomic-conn))

(def our-user-query-result
  (d/q
   '[:find ?user
     :where [?user :user/email "august@augustl.com"]]
   our-db))

(def our-user (d/entity our-db (ffirst our-user-query-result)))
(d/touch our-user)



(def update-tx
  (deref
   (d/transact
    datomic-conn
    [[:db/add (:db/id our-user) :user/email "august@kodemaker.no"]])))



(def retract-tx
  (deref
   (d/transact
    datomic-conn
    [[:db.fn/retractEntity (:db/id our-user)]])))



(d/resolve-tempid (:db-after insert-tx) (:tempids insert-tx) user-tempid)



(deref
 (d/transact
  datomic-conn
  [{:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :user/orders
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :order/products
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :product/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :product/price
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one}

   {:db/id #db/id[:db.part/db]
    :db.install/_attribute :db.part/db
    :db/ident :delivery/order
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}]))



(do
  (def our-user (util/insert datomic-conn
                             [[:user/email "newuser@foo.com"]
                              [:user/password-hash "123abc"]]))
  (def product-a (util/insert datomic-conn
                              [[:product/name "Product A"]
                               [:product/price (bigdec 150)]]))
  (def product-b (util/insert datomic-conn
                              [[:product/name "Product B"]
                               [:product/price (bigdec 100)]]))
  (def product-c (util/insert datomic-conn
                              [[:product/name "Product C"]
                               [:product/price (bigdec 76)]])))



(let [order-tempid (d/tempid :db.part/user)]
  (def order-tx
    @(d/transact
      datomic-conn
      [[:db/add (:db/id our-user) :user/orders order-tempid]
       [:db/add order-tempid :order/products (:db/id product-a)]
       [:db/add order-tempid :order/products (:db/id product-c)]]))
  (def order (util/get-inserted-entity order-tx order-tempid)))



(let [delivery-tempid (d/tempid :db.part/user)]
  (def delivery-tx
    @(d/transact
      datomic-conn
      [[:db/add delivery-tempid :delivery/order (:db/id order)]]))
  (def delivery (util/get-inserted-entity delivery-tx delivery-tempid)))



(deref
 (d/transact
  datomic-conn
  [[:db/add (:db/id our-user) :user/email "newuser-changed@foo.com"]]))

(def our-db (d/db datomic-conn))
(def our-user (d/entity our-db (:db/id our-user)))



;; entity attribute value tx added?
(def delivery-txs
  (d/q '[:find ?tx :in $ ?order-id
         :where [?order-id _ _ ?tx]] (d/history our-db) (:db/id delivery)))

;; We know there's only one..
(def delivery-tx (d/touch (d/entity our-db (ffirst delivery-txs))))



(def user-as-of-delivery
  (d/entity
   (d/as-of our-db (:db/id delivery-tx))
   (:db/id our-user)))