(ns permission.update-permission-description-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.controller :as controller]
            [iam-clj-api.permission.model :as model]
            [lib.response :refer [error success]]))

(defn setup [f]
  (model/drop-permission-table)
  (model/create-permission-table)
  (model/insert-permission {:name "test-permission" :description "test description"})
  (f))

(use-fixtures :each setup)


(deftest test-update-permission-description
  (testing "update-permission-description with valid id and new description"
    (let [permission (model/get-permission-by-id 1)
          new-description "New description"]
      (is (= (success 200 "Permission description updated")
             (controller/update-permission-description 1 new-description)))))

  (testing "update-permission-description with invalid id"
    (let [permission (model/get-permission-by-id 1)
          new-description "New description"]
      (is (= (error 404 "Permission not found")
             (controller/update-permission-description 100 new-description)))))

  (testing "update-permission-description with empty new description"
    (let [permission (model/get-permission-by-id 1)
          new-description ""]
      (is (= (success 200 "Permission description updated")
             (controller/update-permission-description 1 new-description))))))