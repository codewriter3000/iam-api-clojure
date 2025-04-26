(ns role.get-role-by-name-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.controller :as controller]
            [iam-clj-api.role.model :as model]
            [lib.core :refer :all]
            [lib.response :refer [error work]]))

(defn setup [f]
    (model/drop-role-table)
    (model/create-role-table)
    (model/insert-role {:name "role1" :description "description1"})
    (model/insert-role {:name "role2" :description "description2"})
    (model/insert-role {:name "role3" :description "description3"})
    (model/insert-role {:name "role4" :description "description4"})
    (model/insert-role {:name "role5" :description "description5"})
    (f))

(use-fixtures :each setup)

(deftest test-get-role-by-name
    (testing "Get role by name"
        (is (= (work 200 {:id 1 :name "role1" :description "description1"})
               (controller/get-role-by-name "role1")))
        (is (= (work 200 {:id 2 :name "role2" :description "description2"})
               (controller/get-role-by-name "role2")))
        (is (= (work 200 {:id 3 :name "role3" :description "description3"})
               (controller/get-role-by-name "role3")))
        (is (= (work 200 {:id 4 :name "role4" :description "description4"})
               (controller/get-role-by-name "role4")))
        (is (= (work 200 {:id 5 :name "role5" :description "description5"})
               (controller/get-role-by-name "role5"))))

    (testing "Get role by name with invalid name"
        (is (= (error 404 "Role not found")
               (controller/get-role-by-name "invalid_role")))))