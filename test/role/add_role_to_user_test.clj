(ns role.add-role-to-user-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.model :as role-model]
            [iam-clj-api.user.model :as user-model]
            [iam-clj-api.role.controller :as role-controller]
            [lib.response :refer [error success work]]))

(defn setup [f]
  (role-model/drop-role-table)
  (role-model/create-role-table)
  (role-model/insert-role {:name "role1" :description "description1"})
  (user-model/drop-user-table)
  (user-model/create-user-table)
  (user-model/insert-user {:username "user1" :email "testuser1@example.com" :password "password1"})
  (f))

(use-fixtures :each setup)

(deftest test-add-role-to-user []
  (testing "Add role to user"
    (is (= (success 200 "Role added to user")
           (role-controller/add-role-to-user 1 1))))

  (testing "Add role to user with invalid role id"
    (is (= (error 404 "Role not found")
           (role-controller/add-role-to-user 100 1))))

  (testing "Add role to user with invalid user id"
    (let [role (role-model/get-role-by-name "role1")]
      (is (= "role1" (get role :name)))
      (is (= (error 404 "User not found")
             (role-controller/add-role-to-user (get role :id) 100))))))