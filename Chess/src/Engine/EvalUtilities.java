import java.util.ArrayList;

public class EvalUtilities {
    public static int[] blackFlippedPosition;

    // board flipping to access black piece position bonus
    static {
        blackFlippedPosition = new int[64];
        int row, col;
        for(int i = 0; i < 64; i++){
            row = Piece.getRow(i);
            col = Piece.getCol(i);

            blackFlippedPosition[i] = ((7 - row) * 8) + col;
        }
    }

    public static final int[] pawnPST = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    public static final int[] knightPST = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    public static final int[] bishopPST = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    public static final int[] rookPST = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };

    public static final int[] queenPST = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    public static final int[] kingMidGamePST = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    public static final int[] kingEndGamePST = {
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };

    /**
     * Static evaluation of a board in its current state
     * @param board refers to the board at a specific position to be evaluated
     * @return the points of a board at the current state
     */
    public static int evaluate(Board board){
        // go through piece list in board and iterate through the pieces
        // get the value of each piece + position bonus points + other evaluation points
        PieceList whitePieces = board.getWhitePieces();
        PieceList blackPieces = board.getBlackPieces();
        Tile[] chessBoard = board.getBoard();

        int whiteEvaluation = calculateScore(whitePieces, chessBoard);
        int blackEvaluation = calculateScore(blackPieces, chessBoard);

        int currentTurnOffset = (board.isWhiteTurn()) ? 1 : -1;

        int boardEvaluation = whiteEvaluation - blackEvaluation;
        return boardEvaluation * currentTurnOffset;
    }

    private static int calculateScore(PieceList pieceList, Tile[] chessBoard){
        int evaluation = 0;
        for(int i = 0; i < pieceList.getCount(); i++){
            evaluation += chessBoard[pieceList.occupiedTiles[i]].getPiece().getValue();
        }
        return evaluation;
    }

    public static void main(String[] args) {
        Board board = new Board();
        String FEN = "8/5P2/8/8/8/4k3/8/K7 w - - 0 1";
        board.init(FEN);
        ArrayList<Short> moves = board.getAllLegalMoves();
        for(Short move : moves){
            Move movement = new Move(board, move);
            movement.makeMove();
            int score = EvalUtilities.evaluate(board);
            System.out.println(score + " " + MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
            movement.unMake();
        }
    }
}
