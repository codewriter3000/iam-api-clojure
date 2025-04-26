(ns permission.get-users-with-permission-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.permission.model :as perm-model]
            [iam-clj-api.user.model :as user-model]
            [iam-clj-api.permission.controller :as controller]
            [lib.response :refer [error]]))

(defn setup [f]
  (perm-model/drop-permission-table)
  (perm-model/create-permission-table)
  (user-model/drop-user-table)
  (user-model/create-user-table)
  (perm-model/insert-permission {:name "test-permission" :description "test description"})
  (user-model/insert-user {:username "testuser1" :email "testuser1@example.com" :password "Password1!"})
  (user-model/insert-user {:username "testuser2" :email "testuser2@example.com" :password "Password1!"})
  (user-model/insert-user {:username "testuser3" :email "testuser3@example.com" :password "Password1!"})
  (f))

(use-fixtures :each setup)

(deftest test-get-users-with-permission []
  (testing "Get users with permission"
    (let [permission (perm-model/get-permission-by-name "test-permission")]
      (is (= "test-permission" (get permission :name)))
      (controller/add-permission-to-user (get permission :id) 1)
      (controller/add-permission-to-user (get permission :id) 2)
      (controller/add-permission-to-user (get permission :id) 3)
      (let [users (get (controller/get-users-with-permission (get permission :id)) :body)]
        (is (= 3 (count users)))
        (is (= "testuser1" (get (first users) :username)))
        (is (= "testuser2" (get (second users) :username)))
        (is (= "testuser3" (get (last users) :username))))))
  (testing "Get users with permission with invalid permission id"
    (let [invalid-permission (controller/get-users-with-permission 100)]
      (is (= (error 404 "Permission not found") invalid-permission))))
  (testing "Get users with permission with no users"
    (perm-model/insert-permission {:name "permission-assigned-to-nobody"})
    (let [permission (perm-model/get-permission-by-name "permission-assigned-to-nobody")]
      (is (= "permission-assigned-to-nobody" (get permission :name)))
      (let [users (get (controller/get-users-with-permission (get permission :id)) :body)]
        (is (= 0 (count users)))))))