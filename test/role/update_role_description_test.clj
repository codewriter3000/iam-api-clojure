(ns role.update-role-description-test
  (:require [clojure.test :refer :all]
            [iam-clj-api.role.controller :as controller]
            [iam-clj-api.role.model :as model]))

(defn setup [f]
    (model/drop-role-table)
    (model/create-role-table)
    (model/insert-role {:name "role1" :description "description1"})
    (f))

(use-fixtures :each setup)

(deftest test-update-role-description
  (testing "update-role-description with valid id and new description"
    (let [role (model/get-role-by-id 1)
          new-description "New description"]
      (is (= {:status 200 :body "Role description updated"}
             (controller/update-role-description 1 new-description)))))

  (testing "update-role-description with invalid id"
    (let [role (model/get-role-by-id 1)
          new-description "New description"]
      (is (= {:status 404 :error "Role not found"}
             (controller/update-role-description 100 new-description)))))

  (testing "update-role-description with empty new description"
    (let [role (model/get-role-by-id 1)
          new-description ""]
      (is (= {:status 200 :body "Role description updated"}
             (controller/update-role-description 1 new-description))))))