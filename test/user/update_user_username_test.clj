(ns user.update-user-username-test
  (:require
   [clojure.test :refer :all]
   [iam-clj-api.user.controller :refer :all]
   [iam-clj-api.user.model :as model]
   [lib.response :refer [success]]))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (model/insert-user {:username "test1" :email "test1@example.com" :password "Password1!"})
  (f))

(use-fixtures :each setup)

(deftest test-update-user-username
  (testing "Update user username"
    (let [result (update-user-username 1 "new-username")]
      (is (= (success 200 "Username updated successfully") result)))))