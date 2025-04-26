(ns permission.get-permissions-by-id-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.controller :as controller]
            [iam-clj-api.permission.model :as model]
            [lib.core :refer :all]
            [lib.response :refer [error success work]]))

(defn setup [f]
    (model/drop-permission-table)
    (model/create-permission-table)
    (model/insert-permission {:name "permission1" :description "description1"})
    (model/insert-permission {:name "permission2" :description "description2"})
    (model/insert-permission {:name "permission3" :description "description3"})
    (model/insert-permission {:name "permission4" :description "description4"})
    (model/insert-permission {:name "permission5" :description "description5"})
    (model/insert-permission {:name "permission6" :description "description6"})
    (model/insert-permission {:name "permission7" :description "description7"})
    (model/insert-permission {:name "permission8" :description "description8"})
    (model/insert-permission {:name "permission9" :description "description9"})
    (model/insert-permission {:name "permission10" :description "description10"})
    (f))

(use-fixtures :each setup)

(deftest test-get-permission-by-id
  (testing "Get permission by id"
    (is (= (work 200 {:id 1 :name "permission1" :description "description1"})
           (controller/get-permission-by-id 1)))
    (is (= (work 200 {:id 2 :name "permission2" :description "description2"})
           (controller/get-permission-by-id 2)))
    (is (= (work 200 {:id 3 :name "permission3" :description "description3"})
           (controller/get-permission-by-id 3)))
    (is (= (work 200 {:id 4 :name "permission4" :description "description4"})
           (controller/get-permission-by-id 4)))
    (is (= (work 200 {:id 5 :name "permission5" :description "description5"})
           (controller/get-permission-by-id 5))))

  (testing "Get permission by id with invalid id"
    (is (= (error 404 "Permission not found")
           (controller/get-permission-by-id 100)))))