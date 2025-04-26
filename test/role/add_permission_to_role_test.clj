(ns role.add-permission-to-role-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.model :as role-model]
            [iam-clj-api.permission.model :as perm-model]
            [iam-clj-api.role.controller :as controller]
            [lib.response :refer [success error]]))

(defn setup [f]
    (role-model/drop-role-table)
    (role-model/create-role-table)
    (perm-model/drop-permission-table)
    (perm-model/create-permission-table)
    (role-model/insert-role {:name "role1" :description "description1"})
    (perm-model/insert-permission {:name "permission1" :description "description1"})
    (perm-model/insert-permission {:name "permission2" :description "description2"})
    (perm-model/insert-permission {:name "permission3" :description "description3"})
    (f))

(use-fixtures :each setup)

(deftest test-add-permission-to-role []
  (testing "Add permission to role"
    (let [role (role-model/get-role-by-name "role1")
          permission (perm-model/get-permission-by-name "permission1")]
      (is (= "role1" (get role :name)))
      (is (= "permission1" (get permission :name)))
      (is (= 0 (count (role-model/get-permissions-for-role (get role :id)))))
      (is (= (success 200 "Permission added to role")
             (controller/add-permission-to-role (get role :id) (get permission :id))))
      (is (= 1 (count (role-model/get-permissions-for-role (get role :id))))))))

    (testing "Add permission to role with invalid permission id"
        (let [role (role-model/get-role-by-name "role1")]
            (is (= "role1" (get role :name)))
            (is (= (error 404 "Permission not found")
                 (controller/add-permission-to-role (get role :id) 100)))))

    (testing "Add permission to role with invalid role id"
        (let [permission (perm-model/get-permission-by-name "permission1")]
            (is (= "permission1" (get permission :name)))
            (is (= (error 404 "Role not found")
                 (controller/add-permission-to-role 100 (get permission :id))))))