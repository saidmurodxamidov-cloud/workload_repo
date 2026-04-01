Feature: Process Trainer Workload
  As the workload service
  I want to correctly process ADD and DELETE workload requests
  So that trainer monthly summaries are always accurate

  Background:
    Given the workload database is clean

  # ============================================================
  # ADD ACTION - POSITIVE SCENARIOS
  # ============================================================

  Scenario: Add workload for a new trainer creates a new record
    Given no workload exists for username "new.trainer"
    When a workload request is processed with:
      | username   | firstName | lastName | active | trainingDate | duration | actionType |
      | new.trainer | New      | Trainer  | true   | 2024-06-15   | 60       | ADD        |
    Then a workload record exists for "new.trainer" in year 2024 month 6 with duration 60

  Scenario: Add workload to an existing trainer accumulates duration
    Given a trainer workload exists for username "john.doe" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | John      | Doe      | true   | 2024 | 3     | 90            |
    When a workload request is processed with:
      | username | firstName | lastName | active | trainingDate | duration | actionType |
      | john.doe | John      | Doe      | true   | 2024-03-20   | 60       | ADD        |
    Then a workload record exists for "john.doe" in year 2024 month 3 with duration 150

  Scenario: Add workload for a different month creates a separate record
    Given a trainer workload exists for username "john.doe" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | John      | Doe      | true   | 2024 | 1     | 60            |
    When a workload request is processed with:
      | username | firstName | lastName | active | trainingDate | duration | actionType |
      | john.doe | John      | Doe      | true   | 2024-02-10   | 45       | ADD        |
    Then a workload record exists for "john.doe" in year 2024 month 1 with duration 60
    And a workload record exists for "john.doe" in year 2024 month 2 with duration 45

  # ============================================================
  # DELETE ACTION - POSITIVE SCENARIOS
  # ============================================================

  Scenario: Delete action reduces the duration correctly
    Given a trainer workload exists for username "john.doe" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | John      | Doe      | true   | 2024 | 3     | 120           |
    When a workload request is processed with:
      | username | firstName | lastName | active | trainingDate | duration | actionType |
      | john.doe | John      | Doe      | true   | 2024-03-10   | 60       | DELETE     |
    Then a workload record exists for "john.doe" in year 2024 month 3 with duration 60

  Scenario: Delete action does not go below zero duration
    Given a trainer workload exists for username "john.doe" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | John      | Doe      | true   | 2024 | 3     | 30            |
    When a workload request is processed with:
      | username | firstName | lastName | active | trainingDate | duration | actionType |
      | john.doe | John      | Doe      | true   | 2024-03-10   | 90       | DELETE     |
    Then a workload record exists for "john.doe" in year 2024 month 3 with duration 0

  # ============================================================
  # IDEMPOTENCY - NEGATIVE SCENARIOS
  # ============================================================

  Scenario: Duplicate idempotency key is ignored and not processed twice
    Given no workload exists for username "dup.trainer"
    When a workload request is processed with idempotency key "key-abc-123" and:
      | username    | firstName | lastName | active | trainingDate | duration | actionType |
      | dup.trainer | Dup       | Trainer  | true   | 2024-05-01   | 60       | ADD        |
    And the same request is processed again with idempotency key "key-abc-123" and:
      | username    | firstName | lastName | active | trainingDate | duration | actionType |
      | dup.trainer | Dup       | Trainer  | true   | 2024-05-01   | 60       | ADD        |
    Then a workload record exists for "dup.trainer" in year 2024 month 5 with duration 60

  Scenario: Same request with a different idempotency key is processed
    Given no workload exists for username "trainer.x"
    When a workload request is processed with idempotency key "key-001" and:
      | username  | firstName | lastName | active | trainingDate | duration | actionType |
      | trainer.x | Test      | User     | true   | 2024-07-10   | 30       | ADD        |
    And a workload request is processed with idempotency key "key-002" and:
      | username  | firstName | lastName | active | trainingDate | duration | actionType |
      | trainer.x | Test      | User     | true   | 2024-07-10   | 30       | ADD        |
    Then a workload record exists for "trainer.x" in year 2024 month 7 with duration 60

  Scenario: Request with null idempotency key is always processed
    Given no workload exists for username "no.key.trainer"
    When a workload request is processed with no idempotency key and:
      | username        | firstName | lastName | active | trainingDate | duration | actionType |
      | no.key.trainer  | No        | Key      | true   | 2024-08-01   | 45       | ADD        |
    And a workload request is processed with no idempotency key and:
      | username        | firstName | lastName | active | trainingDate | duration | actionType |
      | no.key.trainer  | No        | Key      | true   | 2024-08-01   | 45       | ADD        |
    Then a workload record exists for "no.key.trainer" in year 2024 month 8 with duration 90