# BLANK Chess Engine

>_BLANK is a UCI chess engine._

This engine was written from scratch based off Object-Oriented design using Java.
The basic  [UCI (Universal Chess Interface) protocol](http://wbec-ridderkerk.nl/html/UCIProtocol.html) has been implemented for communication with UCI-compatible GUIs. It also has an in-built GUI to directly interact with the engine.

## AI Features:
* Opening Book (Built into an `OpeningTrie` using a database of >2600 Elo Games)
* Negamax Search with Alpha-Beta pruning
* Quiescence Search
* Search Extension on check
* Static board evaluation
* Move Ordering (PV Nodes, Refutation Moves, MVV / LVA for Captures, Promotion)
* Transposition Table (Zobrist Hashing)
* Null-Move pruning
* Killer / History Heuristics
* Iterative Deepening Search

## To-Be-Implemented:
* Insufficient material draw
* 50 move rule for AI
* Late Move Reduction
* Aspiration Window

## Installation:

You can download the binary ```BLANK.jar``` from the latest release of this repository.
> This engine was built using Java SE 11. Ensure that you have installed either Java 8 or a later version of the JRE in order to run BLANK.

Navigate to the directory of the saved binary file:
```
cd [file directory] (i.e. "cd C:\Desktop\Chess-Engine").
```
Run the engine:
```
java -jar BLANK.jar
```

To install BLANK Chess as a UCI engine on Arena Chess GUI:
* Download the [Arena GUI](http://www.playwitharena.de/)
* Download the binary of BLANK chess engine from the latest release.
* From the Arena GUI MenuBar, Select 'Engines' > 'Install New Engine'
* Select '.jar' file type and navigate to location of the saved binary file.
* Select the binary file and choose 'UCI protocol'.
* Go to the MenuBar, Select 'Engines' > 'Manage...' > 'Details' and Select BLANK Chess Engine
* Under 'General', Click on the 'Type' drop-down list > select 'UCI'. Apply changes.
* Enjoy the engine!
