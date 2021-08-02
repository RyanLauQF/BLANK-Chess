# Chess-Engine

A Chess engine in the making.

This engine is based off Object-Oriented design using Java.

> _Chess is playable with a User Interface by running `GameManager`._

## AI Features:
* Opening Book (Built into an `OpeningTrie` using a database of >2600 Elo Games)
* Negamax Search with Alpha-Beta pruning
* Static board evaluation
* Move Ordering (PV Nodes, Refutation Moves, MVV / LVA for Captures, Promotion)
* Quiescence Search
* Transposition Table (Zobrist Hashing)
* Null-Move pruning
* Search Extension on check
* Killer / History Heuristics
* Iterative Deepening Search

## Main logic TODO List:
1) Implement 50 move rule for AI

## Engine TODO List:
1) Implement Late Move Reduction
2) Aspiration Window
