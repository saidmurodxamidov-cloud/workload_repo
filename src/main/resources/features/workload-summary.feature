Feature: Get Trainer Workload Summary
  As a system consumer
  I want to retrieve a trainer's monthly training summary
  So that I can track how many hours a trainer has worked

  Background:
    Given the workload database is clean

  # ============================================================
  # POSITIVE SCENARIOS
  # ============================================================

  Scenario: Successfully retrieve summary for a trainer with recorded workloads
    Given a trainer workload exists for username "john.doe" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | John      | Doe      | true   | 2024 | 1     | 120           |
      | John      | Doe      | true   | 2024 | 2     | 90            |
    When I request the workload summary for trainer "john.doe"
    Then the response status should be 200
    And the response username should be "john.doe"
    And the response firstName should be "John"
    And the response lastName should be "Doe"
    And the trainer should be active
    And the summary for year 2024 month "January" should be 120
    And the summary for year 2024 month "February" should be 90

  Scenario: Successfully retrieve summary with multiple years of data
    Given a trainer workload exists for username "alice.smith" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | Alice     | Smith    | true   | 2023 | 11    | 60            |
      | Alice     | Smith    | true   | 2024 | 3     | 150           |
    When I request the workload summary for trainer "alice.smith"
    Then the response status should be 200
    And the summary for year 2023 month "November" should be 60
    And the summary for year 2024 month "March" should be 150

  Scenario: Successfully retrieve summary for an inactive trainer
    Given a trainer workload exists for username "bob.jones" with the following data:
      | firstName | lastName | active | year | month | totalDuration |
      | Bob       | Jones    | false  | 2024 | 5     | 45            |
    When I request the workload summary for trainer "bob.jones"
    Then the response status should be 200
    And the trainer should be inactive

  # ============================================================
  # NEGATIVE SCENARIOS
  # ============================================================

  Scenario: Return 404 when trainer has no workload records
    Given no workload exists for username "unknown.trainer"
    When I request the workload summary for trainer "unknown.trainer"
    Then the response status should be 404

  Scenario: Return 404 for a completely empty database
    Given the workload database is clean
    When I request the workload summary for trainer "any.trainer"
    Then the response status should be 404