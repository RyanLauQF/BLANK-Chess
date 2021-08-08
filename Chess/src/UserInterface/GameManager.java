import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GameManager {
    private final ChessGUI chessGUI;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final Board board;

    private final Clock playerOneClock;
    private final Clock playerTwoClock;

    private static final int timePerPlayer = 300;    // 5 minutes per player
    private static final int incrementPerMove = 5;   // 5 seconds increment per move

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
                    move = whitePlayer.iterativeDS(0.5, true);
                    Move movement = new Move(board, move);
                    movement.makeMove();
                }
                else{
                    move = blackPlayer.iterativeDS(0.5, true);
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
                    short move = computerPlayer.iterativeDS(Clock.getTimePerMove(playerTwoClock.getRemainingTime() / 1000, incrementPerMove), true);
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

    /**
     * Sets time control for the game
     * @param totalTimePerPlayer refers to the total time given to each player for the game (in seconds)
     * @param incrementPerMove refers to the bonus time awarded after each move is made (in seconds)
     */
    private void setGameTimeControl(double totalTimePerPlayer, double incrementPerMove){
        // set time control
        playerOneClock.setTimeControl(totalTimePerPlayer, incrementPerMove);
        playerTwoClock.setTimeControl(totalTimePerPlayer, incrementPerMove);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // checks if we want to use UCI protocol
        UCI.UCICommunicate();

        Board board = new Board();
        board.init(FENUtilities.startFEN);
        ChessGUI chessGUI = new ChessGUI(board);

        boolean whitePlayer_isHuman = false;
        boolean blackPlayer_isHuman = true;

        GameManager gameManager = new GameManager(chessGUI, board, whitePlayer_isHuman, blackPlayer_isHuman);
        gameManager.startGame();
    }
}
