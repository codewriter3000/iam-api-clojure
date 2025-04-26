(ns permission.delete-permission-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.model :as model]
            [iam-clj-api.permission.controller :as controller]
            [lib.response :refer [error]]))

(defn setup [f]
    (model/drop-permission-table)
    (model/create-permission-table)
    (model/insert-permission {:name "test-permission" :description "test description"})
    (f))

(use-fixtures :each setup)

(deftest test-delete-permission []
  (testing "Delete permission"
    (let [permission (model/get-permission-by-name "test-permission")]
      (is (= "test-permission" (get permission :name)))
      (controller/delete-permission (get permission :id))
      (is (= (error 404 "Permission not found") (controller/get-permission-by-id (get permission :id)))))))