(ns sns-clj.default-test
  (:require [clojure.test :refer :all]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [sns-clj.core :as sut]))

(use-fixtures :once sut/with-sns-fn)

(defn around-all
  [f]
  (sut/with-sns-fn f))

(use-fixtures :once around-all)

(def sns (aws/client {:api :sns}))

(aws/ops sns)

(def sns (aws/client {:api                  :sns
                      :region "us-east-1"
                      :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id     "ABC"
                                               :secret-access-key "XYZ"})
                      :endpoint-override {:protocol :http
                                          :hostname "localhost"
                                          :port     9911}}))

(deftest can-wrap-around
  (testing "using defaults"
    (is (= {:Topics [{:TopicArn "arn:aws:sns:us-east-1:123456789012:test1"}]} (aws/invoke sns {:op :ListTopics})))))

