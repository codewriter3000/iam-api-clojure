(ns role.get-permissions-for-role-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.model :as role-model]
            [iam-clj-api.permission.model :as perm-model]
            [iam-clj-api.role.controller :as controller]
            [lib.response :refer [error]]))

(defn setup [f]
    (role-model/drop-role-table)
    (role-model/create-role-table)
    (perm-model/drop-permission-table)
    (perm-model/create-permission-table)
    (role-model/insert-role {:name "role1" :description "description1"})
    (perm-model/insert-permission {:name "permission1" :description "description1"})
    (perm-model/insert-permission {:name "permission2" :description "description2"})
    (perm-model/insert-permission {:name "permission3" :description "description3"})
    (role-model/add-permission-to-role 1 1)
    (role-model/add-permission-to-role 2 1)
    (role-model/add-permission-to-role 3 1)
    (f))

(use-fixtures :each setup)

(deftest test-get-permissions-for-role
  (testing "Get permissions for role"
    (let [role (role-model/get-role-by-name "role1")]
      (is (= "role1" (get role :name)))
      (is (= (get (controller/get-permissions-for-role (get role :id)) :status) 200))
      (let [permissions (get (controller/get-permissions-for-role (get role :id)) :body)]
        (is (= 3 (count permissions)))
        (is (= "permission1" (get (first permissions) :name)))
        (is (= "permission2" (get (second permissions) :name)))
        (is (= "permission3" (get (last permissions) :name))))))
  (testing "Get permissions for role with invalid role id"
    (let [invalid-role (controller/get-permissions-for-role 100)]
      (is (= (error 404 "Role not found") invalid-role))))
  (testing "Get permissions for role with no permissions"
    (role-model/insert-role {:name "role-with-no-permissions" :description "description1"})
    (let [role (role-model/get-role-by-name "role-with-no-permissions")]
      (is (= "role-with-no-permissions" (get role :name)))
      (let [permissions (get (controller/get-permissions-for-role (get role :id)) :body)]
        (is (= 0 (count permissions)))))))