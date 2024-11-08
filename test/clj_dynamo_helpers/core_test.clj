(ns clj-dynamo-helpers.core-test
  (:require [clojure.test :refer :all]
            [clj-dynamo-helpers.core :refer [map->attributevalue dynamo->clojure]]
            [clojure.java.io :as io])
  (:import (java.nio ByteBuffer)))

(deftest map->attributevalue-test
  (testing "Basic scalar types"
    (are [input expected] (= expected (map->attributevalue input))
      {"string" "hello"}     {:string {:S "hello"}}
      {"number" 42}         {:number {:N "42"}}
      {"float" 3.14}        {:float {:N "3.14"}}
      {"bool" true}         {:bool {:BOOL true}}
      {"nil" nil}           {:nil {:NULL true}}))
  
  (testing "Collections"
    (are [input expected] (= expected (map->attributevalue input))
      {"list" ["a" "b"]}    {:list {:L [{:S "a"} {:S "b"}]}}
      {"string-set" #{"a" "b"}} {:string-set {:SS ["a" "b"]}}
      {"number-set" #{1 2}}     {:number-set {:NS ["1" "2"]}}))
  
  (testing "Nested structures"
    (let [nested {"map" {"a" 1 "b" "two"}}
          expected {:map {:M {:a {:N "1"} :b {:S "two"}}}}]
      (is (= expected (map->attributevalue nested))))
    
    (let [nested-list {"items" [{"id" 1} {"id" 2}]}
          expected {:items {:L [{:M {:id {:N "1"}}} {:M {:id {:N "2"}}}]}}]
      (is (= expected (map->attributevalue nested-list)))))
  
  (testing "Binary data"
    (let [bytes (.getBytes "hello")
          buffer (ByteBuffer/wrap bytes)]
      (are [input expected] (= expected (map->attributevalue input))
        {"bytes" bytes}     {:bytes {:B bytes}}
        {"buffer" buffer}   {:buffer {:B buffer}}))))

(deftest dynamo->clojure-test
  (testing "si example"
    (are [input expected] (= expected (dynamo->clojure input))
                          {:email {:S "zxzx@yahoo.com"}
                           :userid {:S "7ds798d89s98d79s87d89s798d789s7"}
                           :abilities {:M {:role {:S "admin_userview"}}}
                           :oemconfig {:M {:upsell_prompted {:L [{:S "wibble"}]}}}}

                          {:abilities {:role "admin_userview"}
                           :email     "zxzx@yahoo.com"
                           :oemconfig {:upsell_prompted ["wibble"]}
                           :userid    "7ds798d89s98d79s87d89s798d789s7"}))


  (testing "Basic scalar types"
    (are [input expected] (= expected (dynamo->clojure input))
      {:string {:S "hello"}}     {:string "hello"}
      {:number {:N "42"}}        {:number 42}
      {:bool {:BOOL true}}       {:bool true}
      {:nil {:NULL true}}        {:nil nil}))
  
  (testing "Collections"
    (are [input expected] (= expected (dynamo->clojure input))
      {:list {:L [{:S "a"} {:S "b"}]}}          {:list ["a" "b"]}
      {:string-set {:SS ["a" "b"]}}             {:string-set #{"a" "b"}}
      {:number-set {:NS ["1" "2"]}}             {:number-set #{1 2}}))
  
  (testing "Nested structures"
    (let [input {:map {:M {:a {:N "1"} :b {:S "two"}}}}
          expected {:map {:a 1 :b "two"}}]
      (is (= expected (dynamo->clojure input))))
    
    (let [input {:items {:L [{:M {:id {:N "1"}}} {:M {:id {:N "2"}}}]}}
          expected {:items [{:id 1} {:id 2}]}]
      (is (= expected (dynamo->clojure input)))))
  
  (testing "Binary data"
    (let [bytes (.getBytes "hello")
          buffer (ByteBuffer/wrap bytes)]
      (are [input expected] (= expected (dynamo->clojure input))
        {:bytes {:B bytes}}     {:bytes bytes}
        {:buffer {:B buffer}}   {:buffer buffer}))))

(deftest roundtrip-test
  (testing "Data survives roundtrip conversion"
    (let [original {:string "hello"
                   :number 42
                   :float 3.14
                   :bool true
                   :nil nil
                   :list ["a" "b"]
                   :nested {:x 1 :y "two"}
                   :complex [{:id 1} {:id 2}]}]
      (is (= original
             (-> original
                 map->attributevalue
                 dynamo->clojure)))))) 