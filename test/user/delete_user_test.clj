(ns user.delete-user-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.user.controller :refer :all]
            [iam-clj-api.user.model :as model]
            [clojure.tools.logging :as log]))

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
      (is (= 200 (:status result))))
    (testing "Delete user that does not exist"
        (let [result (delete-user 2)]
            (is (= 404 (:status result)))))))