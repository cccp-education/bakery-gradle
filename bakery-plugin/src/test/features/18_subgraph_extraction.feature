@cucumber @bakery @lens-subgraph
Feature: Subgraph Extraction — BKY-LENS-1

  As a formateur using bakery
  I want the lens to extract a focused subgraph from the global workspace graph
  So that my site only references relevant workspace nodes instead of 203k unrelated ones

  Background:
    Given a new Bakery project

  Scenario: Filtering by community returns only nodes from targeted communities
    Given a graph.json file with 6 nodes in 2 communities and 5 edges
    When I extract a subgraph with communities "community-0"
    Then the subgraph should only contain nodes from community "community-0"

  Scenario: Scope FULL returns all nodes without community filtering
    Given a graph.json file with 6 nodes in 2 communities and 5 edges
    When I extract a subgraph with scope "FULL"
    Then the subgraph should contain nodes from all communities

  Scenario: Extracting from a custom graph file path
    Given a graph.json file at custom path "custom/graph.json" with 5 nodes
    When I extract a subgraph with communities "custom-community"
    Then the subgraph should contain 5 nodes