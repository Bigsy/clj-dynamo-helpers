(ns clj-dynamo-helpers.core
  (:import (java.nio ByteBuffer)))

(defn map->attributevalue [m]
  (into {}
        (for [[k v] m]
          [(keyword k)
           (cond
             (map? v) {:M (map->attributevalue v)}
             (vector? v) {:L (mapv (fn [item]
                                   (cond
                                     (map? item) {:M (map->attributevalue item)}
                                     :else {:S (str item)})) v)}
             (integer? v) {:N (str v)}
             (float? v) {:N (str v)}
             (boolean? v) {:BOOL v}
             (nil? v) {:NULL true}
             (bytes? v) {:B v}
             (instance? ByteBuffer v) {:B v}
             (and (set? v) (every? string? v)) {:SS (vec v)}
             (and (set? v) (every? number? v)) {:NS (vec (map str v))}
             :else {:S (str v)})])))

(defn dynamo->clojure [m]
  (into {} (for [[k v] m]
             [k (let [[type value] (first v)]
                  (case type
                    :S value
                    :N (if (re-find #"\." value)
                         (Double/parseDouble value)
                         (Integer/parseInt value))
                    :BOOL value
                    :NULL nil
                    :B value
                    :SS (set value)
                    :NS (set (map #(if (re-find #"\." %)
                                   (Double/parseDouble %)
                                   (Integer/parseInt %)) value))
                    :BS (set value)
                    :L (mapv (fn [item]
                             (let [[item-type item-value] (first item)]
                               (case item-type
                                 :M (dynamo->clojure item-value)
                                 :S item-value
                                 :N (if (re-find #"\." item-value)
                                     (Double/parseDouble item-value)
                                     (Integer/parseInt item-value))
                                 item-value))) value)
                    :M (dynamo->clojure value)
                    value))])))