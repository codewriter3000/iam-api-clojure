(ns user.update-user-email-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.user.controller :refer :all]
            [iam-clj-api.user.model :as model]
            [lib.response :refer [success]]))

(defn setup [f]
    (model/drop-user-table)
    (model/create-user-table)
    (model/insert-user {:username "test1" :email "test1@example.com" :password "Password1!"})
    (f))

(use-fixtures :each setup)

(deftest test-update-user-email
  (testing "Update user email"
    (let [result (update-user-email 1 "newemail@example.com")]
      (is (= (success 200 "Email updated successfully") result)))))