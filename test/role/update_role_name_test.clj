(ns role.update-role-name-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.controller :as controller]
            [iam-clj-api.role.model :as model]
            [lib.core :refer :all]))

(defn setup [f]
    (model/drop-role-table)
    (model/create-role-table)
    (model/insert-role {:name "role1" :description "description1"})
    (f))

(use-fixtures :each setup)

(deftest test-update-role-name
    (testing "Update role name"
        (let [role (model/get-role-by-name "role1")]
        (is (= "role1" (get role :name)))
        (controller/update-role-name (get role :id) "new-role1")
        (let [updated-role (model/get-role-by-name "new-role1")]
            (is (= "new-role1" (get updated-role :name))))))
    )

(deftest test-update-role-name-with-empty-name []
    (testing "Update role name with empty name"
        (let [role (model/get-role-by-name "role1")]
        (is (= "role1" (get role :name)))
        (is (= {:status 400 :error "Missing new name"} (controller/update-role-name (get role :id) ""))))))

(deftest test-update-role-name-with-invalid-id []
    (testing "Update role name with invalid id"
        (let [role (model/get-role-by-name "role1")]
        (is (= "role1" (get role :name)))
        (is (= {:status 404 :error "Role not found"} (controller/update-role-name 100 "new-role1"))))))