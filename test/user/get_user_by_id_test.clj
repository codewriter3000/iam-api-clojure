(ns user.get-user-by-id-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.user.controller :refer :all] ;;replace with model if this doesn't work
            [iam-clj-api.user.model :as model]))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (model/insert-user {:username "test1" :email "test1@example.com" :password "Password1!"})
  (model/insert-user {:username "test2" :email "test2@example.com" :password "Password1!"})
  (model/insert-user {:username "test3" :email "test3@example.com" :password "Password1!"})
  (model/insert-user {:username "test4" :email "test4@example.com" :password "Password1!"})
  (model/insert-user {:username "test5" :email "test5@example.com" :password "Password1!"})
  (f))

(use-fixtures :each setup)

(deftest test-get-user-by-id
  (testing "get-user-by-id returns a user by id"
    (let [user (model/get-user-by-id 4)
          id (get user :id)]
      (is (= {:status 200 :body user}
             (get-user-by-id id))))))