import javax.swing.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class GameManager {
    private final ChessGUI chessGUI;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final Board board;

    private final Clock playerOneClock;
    private final Clock playerTwoClock;

    // GAME SETTINGS
    private static int timePerPlayer = 300;    // 5 minutes per player
    private static int incrementPerMove = 5;   // 5 seconds increment per move
    private static boolean whitePlayer_isHuman = true;
    private static boolean blackPlayer_isHuman = false;

    public GameManager(ChessGUI chessGUI, Board board, boolean p1_isHuman, boolean p2_isHuman) throws IOException {
        this.chessGUI = chessGUI;
        this.whitePlayer = new Player(true, p1_isHuman, board);
        this.blackPlayer = new Player(false, p2_isHuman, board);
        this.board = board;
        this.playerOneClock = new Clock();
        this.playerTwoClock = new Clock();
    }

    public void startGame() throws InterruptedException {
        // initiate the chessGUI
        chessGUI.initGUI();

        // AI vs AI
        if(!whitePlayer.isHuman() && !blackPlayer.isHuman()){
            boolean playerToMove;
            TimeUnit.MILLISECONDS.sleep(1500);
            while(board.getAllLegalMoves().size() != 0){
                playerToMove = board.isWhiteTurn();
                short move;
                if(whitePlayer.isWhite() == playerToMove){
                    move = whitePlayer.searchMove(true, 0.5);
                    Move movement = new Move(board, move);
                    movement.makeMove();
                }
                else{
                    move = blackPlayer.searchMove(true, 0.5);
                    Move movement = new Move(board, move);
                    movement.makeMove();
                }
                chessGUI.update();
                chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));

                // store zobrist hash of board after AI moved into repetition history
                long zobristHash = board.getZobristHash();
                byte repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                if(board.getBlackPieces().getCount() == 1 && board.getWhitePieces().getCount() == 1 || board.repetitionHistory.get(zobristHash) == 3){
                    break;
                }
            }
            if(GameStatus.checkGameEnded(board)){
                String gameState = GameStatus.getHowGameEnded();
                JOptionPane.showMessageDialog(chessGUI, gameState,
                        "Game Manager", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        else if ((!whitePlayer.isHuman() && blackPlayer.isHuman()) || (whitePlayer.isHuman() && !blackPlayer.isHuman())){
            Player computerPlayer;

            if(!whitePlayer.isHuman()){
                computerPlayer = whitePlayer;   // if the AI is the white player
            }
            else{
                computerPlayer = blackPlayer;   // if the AI is the black player
            }

            // sets time control for the game
            setGameTimeControl(timePerPlayer, incrementPerMove);

            boolean playerPrompted = false;
            do {
                if (computerPlayer.isWhite() == board.isWhiteTurn()) {
                    if(chessGUI.isPlayerIsPromoting()) {
                        TimeUnit.MILLISECONDS.sleep(500);
                        continue;
                    }
                    playerTwoClock.start();
                    // store zobrist hash of board after player has moved into repetition history
                    long zobristHash = board.getZobristHash();
                    int repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                    System.out.println(repetitionCount);
                    board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                    // computer makes move
                    System.out.println("Engine is thinking...");
                    short move = computerPlayer.searchMove(true, Clock.getTimePerMove(playerTwoClock.getRemainingTime() / 1000, incrementPerMove));
                    Move movement = new Move(board, move);
                    movement.makeMove();

                    // store zobrist hash of board after AI moved into repetition history
                    zobristHash = board.getZobristHash();
                    repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                    board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                    chessGUI.update();
                    chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));
                    playerPrompted = false;
                    playerTwoClock.pause();
                    playerTwoClock.incrementTime();
                    System.out.println("Remaining Time: " + playerTwoClock.getRemainingTime());
                } else {
                    if(!playerPrompted){
                        System.out.println("Waiting for Player move...");
                        playerPrompted = true;
                    }
                    while (true) {
                        if (board.isWhiteTurn() != computerPlayer.isWhite()) {
                            TimeUnit.MILLISECONDS.sleep(1000);
                            break;
                        }
                    }
                }
            } while ((board.getBlackPieces().getCount() + board.getWhitePieces().getCount() != 2) &&
                    board.getAllLegalMoves().size() != 0);

            if(GameStatus.checkGameEnded(board)){
                String gameState = GameStatus.getHowGameEnded();
                JOptionPane.showMessageDialog(chessGUI, gameState,
                        "Game Manager", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        else{
            // human player vs human player
            // check if the game has ended after every move is made on the board by the players
            chessGUI.setCheckGameEndAfterEachMove(true);
        }
    }

    private static void getGameINFO(){
        System.out.println("\nGame Settings\n" +
                "----------------------------------------------------------\n" +
                "Bullet:\n" +
                "1) 1 | 0\n" +
                "2) 2 | 1\n" +
                "\n" +
                "Blitz:\n" +
                "3) 3 | 0\n" +
                "4) 3 | 2\n" +
                "5) 5 | 0\n" +
                "6) 5 | 5\n" +
                "7) 10 | 0\n" +
                "\n" +
                "Custom:\n" +
                "Type '0' for custom time control\n");

        System.out.println("i.e. 3|2 - represents a game of 3 minutes per player with 2 seconds increment per turn.\n" +
                "Input the respective indices to select desired time control.\n" +
                "\n* Note that time control only applies to BLANK currently\n");

        Scanner sc = new Scanner(System.in);
        int timeControlIndex;
        do {
            System.out.print("Select time control: ");
            while(!sc.hasNextInt()){    // check for valid input
                System.out.println("Invalid Input! (0 - 7)");
                System.out.print("Select time control: ");
                sc.next();
            }
            timeControlIndex = sc.nextInt();
            switch(timeControlIndex) {
                case 1:
                    timePerPlayer = 60;
                    incrementPerMove = 0;
                    break;

                case 2:
                    timePerPlayer = 120;
                    incrementPerMove = 1;
                    break;

                case 3:
                    timePerPlayer = 180;
                    incrementPerMove = 0;
                    break;

                case 4:
                    timePerPlayer = 180;
                    incrementPerMove = 2;
                    break;

                case 5:
                    timePerPlayer = 300;
                    incrementPerMove = 0;
                    break;

                case 6:
                    timePerPlayer = 300;
                    incrementPerMove = 5;
                    break;

                case 7:
                    timePerPlayer = 600;
                    incrementPerMove = 0;
                    break;

                case 0:
                    System.out.println("\nCustom Settings:\n" + "----------------------------------------------------------");
                    Scanner scanner = new Scanner(System.in);
                    int customTimePerPlayer;
                    do {
                        System.out.print("Total time for each player (in seconds): ");
                        while (!scanner.hasNextInt()) {
                            System.out.println("Invalid duration!");
                            scanner.next();
                            System.out.print("Total time for each player (in seconds): ");
                        }
                        customTimePerPlayer = scanner.nextInt();
                    } while (customTimePerPlayer <= 0);

                    System.out.println();
                    int customIncrement;
                    do {
                        System.out.print("Bonus time after each turn (in seconds): ");
                        while (!scanner.hasNextInt()) {
                            System.out.println("Invalid timing!");
                            scanner.next();
                            System.out.print("Bonus time after each turn (in seconds): ");
                        }
                        customIncrement = scanner.nextInt();
                    } while (customIncrement < 0);
                    System.out.println();

                    // set to custom time controls
                    timePerPlayer = customTimePerPlayer;
                    incrementPerMove = customIncrement;
                    break;
            }
        }
        while(timeControlIndex < 0 || timeControlIndex > 7);

        // Get player side
        int modeSelection;
        System.out.println("\n\nModes:\n" +
                "----------------------------------------------------------\n" +
                "1) Play as White\n" +
                "2) Play as Black\n" +
                "3) Computer Vs Computer (BLANK will play against itself at 500ms per move)\n");

        do{
            System.out.print("Select Mode: ");
            sc = new Scanner(System.in);
            modeSelection = sc.nextInt();
        }
        while(modeSelection < 1 || modeSelection > 3);

        switch (modeSelection){
            case 1:
                whitePlayer_isHuman = true;
                blackPlayer_isHuman = false;
                break;
            case 2:
                whitePlayer_isHuman = false;
                blackPlayer_isHuman = true;
            case 3:
                whitePlayer_isHuman = false;
                blackPlayer_isHuman = false;
        }
    }

    /**
     * Sets time control for the game
     * @param totalTimePerPlayer refers to the total time given to each player for the game (in seconds)
     * @param incrementPerMove refers to the bonus time awarded after each move is made (in seconds)
     */
    private void setGameTimeControl(double totalTimePerPlayer, double incrementPerMove){
        // set time control
        playerOneClock.setTimeControl(totalTimePerPlayer, incrementPerMove);
        playerTwoClock.setTimeControl(totalTimePerPlayer, incrementPerMove);

        System.out.println("Game has Started!\n" +
                "----------------------------------------------------------\n" +
                "Time Control:\n" +
                "Time Per Player: " + totalTimePerPlayer + " seconds\n" +
                "Bonus Time Per Move: " + incrementPerMove + " seconds\n" +
                "----------------------------------------------------------\n");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // checks if we want to use UCI protocol
        new UCI();

        // time control setting
        GameManager.getGameINFO();

        // if user types in "gui" in UCI protocol, local gui will be initiated
        Board board = new Board();
        board.init(FENUtilities.startFEN);
        ChessGUI chessGUI = new ChessGUI(board);

        GameManager gameManager = new GameManager(chessGUI, board, whitePlayer_isHuman, blackPlayer_isHuman);
        gameManager.startGame();
    }
}
