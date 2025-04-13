(ns user.insert-user-test
  (:require
   [clojure.test :refer :all]
   [iam-clj-api.user.controller :refer :all]
   [iam-clj-api.user.model :as model]
   [lib.response :refer [error success]]))

(defn mock-get-user-by-username [username]
  (if (= username "existinguser")
    {:username "existinguser"}
    nil))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (with-redefs [model/get-user-by-username mock-get-user-by-username]
    (f)))

(use-fixtures :each setup)

(deftest test-insert-user
  (testing "Insertion of user"
    (is (= (success 201 "User created successfully")
           (insert-user {:username "newuser" :email "newuser@example.com" :password "Password1!"})))))

(deftest test-validate-input
  (testing "Validation of input"
    (is (= (error 400 "All fields are required")
           (validate-input {:username "" :email "test@example.com" :password "Password1!"})))

    (is (= (error 400 "Password must be at least 8 characters long, contain an uppercase letter, a lowercase letter, a number, and a special character")
           (validate-input {:username "username" :email "test@example.com" :password "password"})))

    (is (= (error 400 "Username must be between 3 and 20 characters")
           (validate-input {:username "ab" :email "test@example.com" :password "Password1!"})))

    (is (= (error 400 "Email is invalid")
           (validate-input {:username "username" :email "invalid-email" :password "Password1!"})))

    (is (= (error 400 "Username already exists")
           (validate-input {:username "existinguser" :email "test@example.com" :password "Password1!"})))))