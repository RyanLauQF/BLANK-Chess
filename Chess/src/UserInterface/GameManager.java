import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GameManager {
    private final ChessGUI chessGUI;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final Board board;

    public GameManager(ChessGUI chessGUI, Board board, boolean p1_isHuman, boolean p2_isHuman) throws IOException {
        this.chessGUI = chessGUI;
        this.whitePlayer = new Player(true, p1_isHuman, board);
        this.blackPlayer = new Player(false, p2_isHuman, board);
        this.board = board;
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
                    move = whitePlayer.getBestMove(4, true);
                    Move movement = new Move(board, move);
                    movement.makeMove();
                }
                else{
                    move = blackPlayer.getBestMove(4, true);
                    Move movement = new Move(board, move);
                    movement.makeMove();
                }
                chessGUI.update();
                chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));

                // store zobrist hash of board after AI moved into repetition history
                long zobristHash = board.getZobristHash();
                byte repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                if(board.getBlackPieces().getCount() == 1 && board.getWhitePieces().getCount() == 1 || board.repetitionHistory.get(board.getZobristHash()) == 3){
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

            boolean playerPrompted = false;
            do {
                if (computerPlayer.isWhite() == board.isWhiteTurn()) {
                    if(chessGUI.isPlayerIsPromoting()) {
                        TimeUnit.MILLISECONDS.sleep(500);
                        continue;
                    }
                    // store zobrist hash of board after player has moved into repetition history
                    long zobristHash = board.getZobristHash();
                    int repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                    board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                    // computer makes move
                    System.out.println("Engine is thinking...");
                    short move = computerPlayer.getBestMove(5, true);
                    Move movement = new Move(board, move);
                    movement.makeMove();

                    // store zobrist hash of board after AI moved into repetition history
                    zobristHash = board.getZobristHash();
                    repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                    board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                    chessGUI.update();
                    chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));
                    playerPrompted = false;
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
            } while ((board.getBlackPieces().getCount() != 1 || board.getWhitePieces().getCount() != 1) &&
                    board.getAllLegalMoves().size() != 0 || board.repetitionHistory.get(board.getZobristHash()) == 3);

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

    public static void main(String[] args) throws IOException, InterruptedException {
        Board board = new Board();
        // Custom FEN input
        String FEN = "r3k2r/p1ppqpb1/Bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPB1PPP/R3K2R b - - 0 1";
        //board.init("8/1p2kp2/6pK/p3b3/4r3/8/8/8 w - - 0 1");
        //board.init("8/8/8/5R2/1p3k2/6pK/1r6/8 b - - 0 1");

        board.init(FENUtilities.startFEN);
        ChessGUI chessGUI = new ChessGUI(board);

        boolean whitePlayer_isHuman = false;
        boolean blackPlayer_isHuman = false;

        GameManager gameManager = new GameManager(chessGUI, board, whitePlayer_isHuman, blackPlayer_isHuman);
        gameManager.startGame();
    }
}
