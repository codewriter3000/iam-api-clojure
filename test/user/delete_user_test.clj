(ns user.delete-user-test
  (:require
   [clojure.test :refer :all]
   [clojure.tools.logging :as log]
   [iam-clj-api.user.controller :refer :all]
   [iam-clj-api.user.model :as model]
   [lib.response :refer [success error]]))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (model/insert-user {:username "test1" :email "test1@example.com" :password "Password1!"})
  (f))

(use-fixtures :each setup)

(deftest test-delete-user
  (testing "Delete user"
    (let [result (delete-user 1)]
      (log/info "Delete user result:" result)
      (is (= (success 204 "User deleted successfully")
             result))))
    (testing "Delete user that does not exist"
      (let [result (delete-user 2)]
        (is (= (error 404 "User not found")
                  result)))))