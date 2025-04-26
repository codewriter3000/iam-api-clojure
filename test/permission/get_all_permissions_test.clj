(ns permission.get-all-permissions-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.controller :as controller]
            [iam-clj-api.permission.model :as model]
            [lib.core :refer :all]
            [lib.response :refer [work]]))

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

(deftest test-get-all-permissions
  (testing "Get all permissions"
    (is (= (work 200 [{:id 1 :name "permission1" :description "description1"}
                               {:id 2 :name "permission2" :description "description2"}
                               {:id 3 :name "permission3" :description "description3"}
                               {:id 4 :name "permission4" :description "description4"}
                               {:id 5 :name "permission5" :description "description5"}
                               {:id 6 :name "permission6" :description "description6"}
                               {:id 7 :name "permission7" :description "description7"}
                               {:id 8 :name "permission8" :description "description8"}
                               {:id 9 :name "permission9" :description "description9"}
                               {:id 10 :name "permission10" :description "description10"}])
           (controller/get-all-permissions)))))