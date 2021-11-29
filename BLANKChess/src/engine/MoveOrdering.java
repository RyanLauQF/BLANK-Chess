import java.io.IOException;
import java.util.ArrayList;

public class MoveOrdering {
    private static final int KING_INDEX = 0;
    private static final int QUEEN_INDEX = 1;
    private static final int ROOK_INDEX = 2;
    private static final int BISHOP_INDEX = 3;
    private static final int KNIGHT_INDEX = 4;
    private static final int PAWN_INDEX = 5;
    private static final int NONE_INDEX = 6;

    // Move ordering scores
    private static final int PV_MOVE_SCORE = 30000;
    private static final int HASH_MOVE_SCORE = 20000;
    private static final int CAPTURE_BONUS = 10000;
    private static final int QUEEN_PROMOTION_BONUS = 9000;
    private static final int FIRST_KILLER = 8000;
    private static final int SECOND_KILLER = 7000;
    private static final int CASTLING_BONUS = 3000;
    private static final int KNIGHT_PROMOTION_BONUS = 1000;
    private static final int UNINTERESTING_PROMOTION = 300;
    private static final int SILENT_MOVE_PENALTY = -1000;

    private static final int[][] MVV_LVA_SCORES = {
            {0, 0, 0, 0, 0, 0, 0},          // victim K, attacker K, Q, R, B, N, P, None
            {50, 51, 52, 53, 54, 55, 0},    // victim Q, attacker K, Q, R, B, N, P, None
            {40, 41, 42, 43, 44, 45, 0},    // victim R, attacker K, Q, R, B, N, P, None
            {30, 31, 32, 33, 34, 35, 0},    // victim B, attacker K, Q, R, B, N, P, None
            {20, 21, 22, 23, 24, 25, 0},    // victim N, attacker K, Q, R, B, N, P, None
            {10, 11, 12, 13, 14, 15, 0},    // victim P, attacker K, Q, R, B, N, P, None
            {0, 0, 0, 0, 0, 0, 0},          // victim None, attacker K, Q, R, B, N, P, None
    };

    public static ArrayList<Short> orderMoves(ArrayList<Short> moves, Search searcher, int searchPly, short ttMove) {
        moves.sort((move1, move2) -> {
            int moveScore1 = getMoveScore(move1, searcher, searchPly, ttMove);
            int moveScore2 = getMoveScore(move2, searcher, searchPly, ttMove);

            return Integer.compare(moveScore2, moveScore1);
        });
        return moves;
    }

    private static int getMoveScore(Short move, Search searcher, int ply, short ttMove){
        Board board = searcher.board;

        // evaluate the move scores
        int score = 0;
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);

        // PV Move
        if(searcher.pvMoveScoring){
            if (searcher.PVMoves[0][ply] == move)
            {
                // disable score PV flag
                searcher.pvMoveScoring = false;

                // give PV move the highest score to search it first
                return PV_MOVE_SCORE;
            }
        }

        // Hash Move
        if(move == ttMove){
            return HASH_MOVE_SCORE;
        }

        if(MoveGenerator.isCastling(move)){
            return CASTLING_BONUS;
        }

        Piece startPiece = board.getTile(start).getPiece();

        // Captures sorted by MVV-LVA
        if(MoveGenerator.isCapture(move)){
            // Most-Valuable Victim / Least-Valuable Aggressor
            if (MoveGenerator.getMoveType(move) == 4) {
                // normal capture
                score += MVV_LVA(board.getTile(end).getPiece(), startPiece);
            }
            else{
                // enpassant capture
                score += MVV_LVA_SCORES[PAWN_INDEX][PAWN_INDEX];  // pawn (victim) - pawn (attacker) capture
            }
            score += CAPTURE_BONUS; // prioritise captures
        }
        // quiet moves positions
        else{
            // killer move
            if(move == searcher.killerMoves[0][ply]){
                score += FIRST_KILLER;
            }
            else if(move == searcher.killerMoves[1][ply]){
                score += SECOND_KILLER;
            }
            else{
               // history move score
                score += searcher.historyMoves[start][end];
            }
        }

        // Promotion bonus score (Queen promotion is prioritised to search)
        if(MoveGenerator.isPromotion(move)){
            int moveType = MoveGenerator.getMoveType(move);
            if(moveType == 8 || moveType == 12){
                // knight promotion
                score += KNIGHT_PROMOTION_BONUS;
            }
            else if(moveType == 11 || moveType == 15){
                // queen promotion
                score += QUEEN_PROMOTION_BONUS;
            }
            else{
                score += UNINTERESTING_PROMOTION; // rook and bishop not as useful as Queen / Knight promotion (knight discovered checks)
            }
        }

        // silent move
        // score using change of Mid-game PSQT values
        if(score == 0){
            int startPos = (startPiece.isWhite()) ? start : EvalUtilities.blackFlippedPosition[start];
            int endPos = (startPiece.isWhite()) ? end : EvalUtilities.blackFlippedPosition[end];

            if(startPiece.isPawn()){
                score += EvalUtilities.pawnMidGamePST[endPos] - EvalUtilities.pawnMidGamePST[startPos];
            }
            else if(startPiece.isBishop()){
                score += EvalUtilities.bishopMidGamePST[endPos] - EvalUtilities.bishopMidGamePST[startPos];
            }
            else if(startPiece.isKnight()){
                score += EvalUtilities.knightMidGamePST[endPos] - EvalUtilities.knightMidGamePST[startPos];
            }
            else if(startPiece.isRook()){
                score += EvalUtilities.rookMidGamePST[endPos] - EvalUtilities.rookMidGamePST[startPos];
            }
            else if(startPiece.isQueen()){
                score += EvalUtilities.queenMidGamePST[endPos] - EvalUtilities.queenMidGamePST[startPos];
            }
            else if(startPiece.isKing()){
                score += EvalUtilities.kingMidGamePST[endPos] - EvalUtilities.kingMidGamePST[startPos];
            }

            score += SILENT_MOVE_PENALTY;
        }

        return score;
    }

    public static ArrayList<Short> orderQuiescence(ArrayList<Short> moves, Search searcher, short bestMove) {
        moves.sort((move1, move2) -> {
            int moveScore1 = getQuiescenceScore(move1, searcher, bestMove);
            int moveScore2 = getQuiescenceScore(move2, searcher, bestMove);

            return Integer.compare(moveScore2, moveScore1);
        });
        return moves;
    }

    private static int getQuiescenceScore(Short move, Search searcher, short bestMove){
        Board board = searcher.board;

        // evaluate the move scores
        int score = 0;
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);

        if(move == bestMove){
            return HASH_MOVE_SCORE;
        }

        Piece startPiece = board.getTile(start).getPiece();

        if(MoveGenerator.isCapture(move)){
            // Sort by Most-Valuable Victim / Least-Valuable Aggressor
            if (MoveGenerator.getMoveType(move) == 4) {
                // normal capture
                score += MVV_LVA(board.getTile(end).getPiece(), startPiece);
            }
            else{
                // enpassant capture
                score += MVV_LVA_SCORES[PAWN_INDEX][PAWN_INDEX];  // pawn (victim) - pawn (attacker) capture
            }
            score += CAPTURE_BONUS; // prioritise captures
        }

        if(MoveGenerator.isPromotion(move)){
            int moveType = MoveGenerator.getMoveType(move);
            if(moveType == 8 || moveType == 12){
                // knight promotion
                score += KNIGHT_PROMOTION_BONUS;
            }
            else if(moveType == 11 || moveType == 15){
                // queen promotion
                score += QUEEN_PROMOTION_BONUS;
            }
            else{
                score += UNINTERESTING_PROMOTION; // rook and bishop not as useful as Queen / Knight promotion (knight discovered checks)
            }
        }

        return score;
    }

    private static int MVV_LVA(Piece victim, Piece attacker){
        int victimINDEX = getPieceMVVLVAIndex(victim.getType());
        int attackINDEX = getPieceMVVLVAIndex(attacker.getType());

        return MVV_LVA_SCORES[victimINDEX][attackINDEX];
    }

    private static int getPieceMVVLVAIndex(Piece.PieceType pieceType){
        if(pieceType == Piece.PieceType.KING){
            return KING_INDEX;
        }
        else if(pieceType == Piece.PieceType.QUEEN){
            return QUEEN_INDEX;
        }
        else if(pieceType == Piece.PieceType.ROOK){
            return ROOK_INDEX;
        }
        else if(pieceType == Piece.PieceType.BISHOP){
            return BISHOP_INDEX;
        }
        else if(pieceType == Piece.PieceType.KNIGHT){
            return KNIGHT_INDEX;
        }
        else if(pieceType == Piece.PieceType.PAWN){
            return PAWN_INDEX;
        }
        // if piece is null, return index 6 which represents none values
        return NONE_INDEX;
    }

    public static void main(String[] args) throws IOException {
        Board board = new Board();
        board.init("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        //board.init("k7/4RP2/n1p2r2/8/p2N4/2P3Pp/1P5P/6K1 w - - 3 46");
        Search searcher = new Search(board, new TranspositionTable());
        searcher.depthSearch(8);

        ArrayList<Short> allMoves = orderMoves(board.getAllLegalMoves(), searcher, 1, (short) 0);
        for(Short moves : allMoves){
            System.out.print(FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(moves)) + "-" + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(moves)) + " ");
            System.out.println("Score: " + getMoveScore(moves, searcher, 1, (short) 0) + " ");
        }
    }
}
