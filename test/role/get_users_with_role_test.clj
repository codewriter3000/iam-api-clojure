(ns role.get-users-with-role-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.model :as role-model]
            [iam-clj-api.user.model :as user-model]
            [iam-clj-api.role.controller :as role-controller]
            [lib.response :refer [error]]))

(defn setup [f]
    (role-model/drop-role-table)
    (role-model/create-role-table)
    (role-model/insert-role {:name "role1" :description "description1"})
    (user-model/drop-user-table)
    (user-model/create-user-table)
    (user-model/insert-user {:username "user1" :email "testuser1@example.com" :password "password1"})
    (user-model/insert-user {:username "user2" :email "testuser2@example.com" :password "password2"})
    (user-model/insert-user {:username "user3" :email "testuser3@example.com" :password "password3"})
    (role-model/add-role-to-user 1 1)
    (role-model/add-role-to-user 1 2)
    (role-model/add-role-to-user 1 3)
    (f))

(use-fixtures :each setup)

(deftest test-get-users-with-role
  (testing "Get users with role"
    (let [role (role-model/get-role-by-name "role1")]
      (is (= "role1" (get role :name)))
      (let [users (get (role-controller/get-users-with-role (get role :id)) :body)]
        (is (= 3 (count users)))
        (is (= "user1" (get (first users) :username)))
        (is (= "user2" (get (second users) :username)))
        (is (= "user3" (get (last users) :username))))))
  (testing "Get users with role with invalid role id"
    (let [invalid-role (role-controller/get-users-with-role 100)]
      (is (= (error 404 "Role not found") invalid-role))))
  (testing "Get users with role with no users"
    (role-model/insert-role {:name "role-assigned-to-nobody" :description "description1"})
    (let [role (role-model/get-role-by-name "role-assigned-to-nobody")]
      (is (= "role-assigned-to-nobody" (get role :name)))
      (let [users (get (role-controller/get-users-with-role (get role :id)) :body)]
        (is (= 0 (count users)))))))