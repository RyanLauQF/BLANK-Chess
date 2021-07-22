# Chess-Engine

A Chess engine in the making.

This engine is based off Object-Oriented design using Java.

Main movement logic for generating all legal moves have been implemented, including promotion to all types.

> _Chess is playable with a User Interface by running `GameManager`._

## AI Features:
* Opening Book (Built into an `OpeningTrie` using a database of >2600 elo Games)
* Negamax Search with Alpha-Beta pruning
* Static board evaluation
* Simple Move Ordering
* Quiescence Search

## Main logic TODO List:
1) Implement 50 move rule, and repetition for AI

## Engine TODO List:
1) Improve on Move ordering
2) Implement Iterative Deepening
