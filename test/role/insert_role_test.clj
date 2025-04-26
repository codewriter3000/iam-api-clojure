(ns role.insert-role-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.controller :as controller]
            [iam-clj-api.role.model :as model]
            [lib.core :refer :all]
            [lib.response :refer [error success]]))

(defn setup [f]
    (model/drop-role-table)
    (model/create-role-table)
    (f))

(use-fixtures :each setup)

(deftest test-insert-role
    (testing "Insert role"
        (is (= (success 201 "Role created successfully")
               (controller/insert-role {:name "role1" :description "description1"})))
        (is (= (success 201 "Role created successfully")
               (controller/insert-role {:name "role2" :description "description2"})))
        (is (= (success 201 "Role created successfully")
               (controller/insert-role {:name "role3" :description "description3"})))
        (is (= (success 201 "Role created successfully")
               (controller/insert-role {:name "role4" :description "description4"})))
    (testing "Insert role with missing description"
        (is (= (success 201 "Role created successfully")
               (controller/insert-role {:name "role5" :description ""}))))
    (testing "Inserting role with duplicate name"
        (is (= (error 422 "Role with name role1 already exists")
               (controller/insert-role {:name "role1" :description "description1"}))))))