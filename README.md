# Chess-Engine

A Chess engine in the making.

This engine is based off Object-Oriented design using Java.

> _Chess is playable with a User Interface by running `GameManager`._

## AI Features:
* Opening Book (Built into an `OpeningTrie` using a database of >2600 elo Games)
* Negamax Search with Alpha-Beta pruning
* Static board evaluation
* Move Ordering (PV Nodes, Refutation Moves, Promotion, MVV / LVA for Captures)
* Quiescence Search - removes horizon effect issues
* Transposition Table (Zobrist Hashing)
* Null-Move pruning
* Search Extention on check

## Main logic TODO List:
1) Implement 50 move rule for AI

## Engine TODO List:
1) Implement Iterative Deepening
2) Implement Late Move Reduction
3) Aspiration Window
