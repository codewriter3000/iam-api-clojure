(ns user.login-user-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.user.controller :refer :all]
            [iam-clj-api.user.model :as model]
            [buddy.hashers :as hashers]
            [lib.response :refer [success]]))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (let [password-hash (hashers/derive "Password1!")
        user {:username "test" :email "test@example.com" :password password-hash}]
    (model/insert-user user)
  (f)))

(use-fixtures :each setup)

(deftest test-login-user
  (testing "Login of user"
    (let [result (login-user "test" "Password1!")]
      (is (= 200 (:status result)))
      (is (= "Login successful" (get-in result [:body :message])))
      (is (= "test" (get-in result [:body :user :username]))))))