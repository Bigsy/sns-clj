(ns sns-clj.sqs-test
  (:require [clojure.test :refer :all]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [sns-clj.core :as sut]
            [cheshire.core :as json]
            [elasticmq-clj.core :as sqs]))

(defn with-around-fns
  [around-fns f]
  (cond
    (empty? around-fns) (f)
    (= 1 (count around-fns)) ((first around-fns) f)
    :else (with-around-fns (butlast around-fns)
                           (fn [] ((last around-fns) f)))))

(defn with-all-deps [f]
  (with-around-fns [ (partial sut/with-sns-fn (.getPath (clojure.java.io/resource "sqs_db.json")))
                     sqs/with-elasticmq-fn ] f))


(use-fixtures :once with-all-deps)


(def sns (aws/client {:api                  :sns
                      :region "us-east-1"
                      :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id     "ABC"
                                               :secret-access-key "XYZ"})
                      :endpoint-override {:protocol :http
                                          :hostname "localhost"
                                          :port     9911}}))

(def sqs (aws/client {:api                  :sqs
                      :region "us-east-1"
                      :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id     "ABC"
                                               :secret-access-key "XYZ"})
                      :endpoint-override {:protocol :http
                                          :hostname "localhost"
                                          :port     9324}}))

(deftest can-wrap-around
  (testing "using defaults"
    (aws/invoke sqs {:op :CreateQueue
                     :request {:QueueName "test-topic"}})
    #_(aws/invoke sqs {:op :SendMessage
                        :request {:QueueUrl "http://localhost:9324/000000000000/test-topic"
                                  :MessageBody "wibblesqs"}})
    (aws/invoke sqs {:op :ListQueues})

    (aws/invoke sns {:op :CreateTopic
                     :request {:Name "test-topic"}})
    (aws/invoke sns {:op :Subscribe
                     :request {:Protocol "sqs"
                               :TopicArn "arn:aws:sns:us-east-1:123456789012:test-topic"
                               :Endpoint "aws-sqs://test-topic?amazonSQSEndpoint=http://localhost:9324&accessKey=&secretKey="}})

    (aws/invoke sns {:op :ListSubscriptions})
    (aws/invoke sns {:op :Publish
                     :request {:Message "wibble"
                               :TopicArn "arn:aws:sns:us-east-1:123456789012:test-topic"}})

    (is (= "wibble" (-> (aws/invoke sqs {:op      :ReceiveMessage
                                         :request {:QueueUrl        "http://localhost:9324/000000000000/test-topic"
                                                   :WaitTimeSeconds 3}})
                        :Messages
                        first
                        :Body
                        (json/parse-string true)
                        :Message)))))

(comment (aws/ops sns)
         (aws/ops sqs))