Feature: Workload JMS Consumer
  As the workload service
  I want to consume workload messages from ActiveMQ
  So that workloads are updated asynchronously without direct HTTP calls

  Background:
    Given the workload database is clean

  # ============================================================
  # POSITIVE SCENARIOS
  # ============================================================

  Scenario: Consumer processes an ADD message and updates workload
    When a JMS message is sent to the workload queue with:
      | username       | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.trainer    | JMS       | Trainer  | true   | 2024-09-10   | 75       | ADD        |
    Then within 5 seconds a workload record exists for "jms.trainer" in year 2024 month 9 with duration 75

  Scenario: Consumer processes a DELETE message and decrements workload
    Given a trainer workload exists for username "jms.trainer2" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | JMS2      | Trainer  | true   | 2024 | 10    | 100           |
    When a JMS message is sent to the workload queue with:
      | username     | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.trainer2 | JMS2      | Trainer  | true   | 2024-10-05   | 40       | DELETE     |
    Then within 5 seconds a workload record exists for "jms.trainer2" in year 2024 month 10 with duration 60

  Scenario: Consumer correctly uses traceId and spanId from message headers
    When a JMS message is sent with traceId "trace-111" spanId "span-222" and body:
      | username       | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.trace.test | Trace     | Test     | true   | 2024-11-01   | 30       | ADD        |
    Then within 5 seconds a workload record exists for "jms.trace.test" in year 2024 month 11 with duration 30

  # ============================================================
  # IDEMPOTENCY VIA JMS
  # ============================================================

  Scenario: Consumer ignores duplicate JMS messages with the same idempotency key
    When a JMS message is sent with idempotency key "jms-idem-key-1" and body:
      | username      | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.idem.user | Idem      | User     | true   | 2024-12-01   | 60       | ADD        |
    And a JMS message is sent with idempotency key "jms-idem-key-1" and body:
      | username      | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.idem.user | Idem      | User     | true   | 2024-12-01   | 60       | ADD        |
    Then within 5 seconds a workload record exists for "jms.idem.user" in year 2024 month 12 with duration 60

  # ============================================================
  # NEGATIVE SCENARIOS
  # ============================================================

  Scenario: Consumer processes message with null idempotency key every time
    When a JMS message is sent with no idempotency key and body:
      | username      | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.null.user | Null      | User     | true   | 2024-06-15   | 50       | ADD        |
    And a JMS message is sent with no idempotency key and body:
      | username      | firstName | lastName | active | trainingDate | duration | actionType |
      | jms.null.user | Null      | User     | true   | 2024-06-15   | 50       | ADD        |
    Then within 5 seconds a workload record exists for "jms.null.user" in year 2024 month 6 with duration 100