(ns permission.add-permission-to-user-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.model :as perm-model]
            [iam-clj-api.permission.controller :as controller]
            [iam-clj-api.user.model :as user-model]
            [lib.response :refer [error]]))

(defn setup [f]
    (perm-model/drop-permission-table)
    (perm-model/create-permission-table)
    (user-model/drop-user-table)
    (user-model/create-user-table)
    (perm-model/insert-permission {:name "test-permission" :description "test description"})
    (user-model/insert-user {:username "testuser1" :email "testuser1@example.com" :password "Password1!"})
    (f))

(use-fixtures :each setup)

(deftest test-add-permission-to-user []
  (testing "Add permission to user"
    (let [permission (perm-model/get-permission-by-name "test-permission")
          user (user-model/get-user-by-username "testuser1")]
      (is (= "test-permission" (get permission :name)))
      (is (= "testuser1" (get user :username)))
      (controller/add-permission-to-user (get permission :id) (get user :id))
      (let [users (get (controller/get-users-with-permission (get permission :id)) :body)]
        (is (= 1 (count users)))
        (is (= "testuser1" (get (first users) :username))))))

  (testing "Add permission to user with invalid permission id"
    (let [user (user-model/get-user-by-username "testuser1")]
      (is (= "testuser1" (get user :username)))
      (is (= (error 404 "Permission not found") (controller/add-permission-to-user 100 (get user :id))))))

  (testing "Add permission to user with invalid user id"
    (let [permission (perm-model/get-permission-by-name "test-permission")]
      (is (= "test-permission" (get permission :name)))
      (is (= (error 404 "User not found") (controller/add-permission-to-user (get permission :id) 100))))))