(ns user.get-all-users-test
  (:require
   [clojure.test :refer :all]
   [iam-clj-api.user.controller :refer :all]
   [iam-clj-api.user.model :as model]
   [lib.response :refer [work]]))

(defn setup [f]
  (model/drop-user-table)
  (model/create-user-table)
  (model/insert-user {:username "test1" :email "test1@example.com" :password "Password1!"})
  (model/insert-user {:username "test2" :email "test2@example.com" :password "Password1!" :first_name "Test" :last_name "User"})
  (model/insert-user {:username "test3" :email "test3@example.com" :password "Password1!" :first_name "Test" :last_name "User"})
  (model/insert-user {:username "test4" :email "test4@example.com" :password "Password1!" :first_name "Test" :last_name "User"})
  (model/insert-user {:username "test5" :email "test5@example.com" :password "Password1!" :first_name "Test" :last_name "User"})
  (f))

(use-fixtures :each setup)

(deftest test-get-all-users
  (testing "Get all users"
    (let [response (get-all-users)
          sanitized-body (map #(dissoc % :created_at) (:body response))] ; Remove :created_at from each user
      (is (= (work 200 [{:id 1 :username "test1" :email "test1@example.com" :first_name nil :last_name nil}
                                 {:id 2 :username "test2" :email "test2@example.com" :first_name "Test" :last_name "User"}
                                 {:id 3 :username "test3" :email "test3@example.com" :first_name "Test" :last_name "User"}
                                 {:id 4 :username "test4" :email "test4@example.com" :first_name "Test" :last_name "User"}
                                 {:id 5 :username "test5" :email "test5@example.com" :first_name "Test" :last_name "User"}])
             (work 200 sanitized-body))))))