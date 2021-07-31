import java.util.ArrayList;

public class MoveOrdering {
    public static ArrayList<Short> orderMoves(ArrayList<Short> moves, Board board, TranspositionTable TT) {
        moves.sort((move1, move2) -> {
            int moveScore1 = getMoveScore(move1, board, TT);
            int moveScore2 = getMoveScore(move2, board, TT);

            return Integer.compare(moveScore1, moveScore2);
        });
        return moves;
    }

    private static int getMoveScore(Short move, Board board, TranspositionTable TT){
        short hashMove = 0;
        byte flag = -1;
        if(TT.containsKey(board.getZobristHash())){
            hashMove = TT.getEntry(board.getZobristHash()).bestMove;
            flag = TT.getEntry(board.getZobristHash()).entry_TYPE;
        }

        // evaluate the move scores
        int score = 0;
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);

        if(move == hashMove){
            if(flag == TranspositionTable.EXACT_TYPE){
                score += 20000; // pv node
            }
            else if(flag == TranspositionTable.UPPERBOUND_TYPE){
                score += 10000; // almost-best move at the current position to cause a cut-off
            }
        }

        boolean isWhitePiece = board.getTile(start).getPiece().isWhite();
        Piece startPiece = board.getTile(start).getPiece();

        if(MoveGenerator.isCapture(move)){
            // Sort by Most Valuable Victim / Least Valuable Aggressor
            if (MoveGenerator.getMoveType(move) == 4) {
                // normal capture
                score += (10 * (board.getTile(end).getPiece().getValue() - startPiece.getValue()));
            }
            else{   // enpassant capture
                score += 10;
            }
        }
        if(MoveGenerator.isPromotion(move)){
            int moveType = MoveGenerator.getMoveType(move);
            if(moveType == 8 || moveType == 12){
                // knight promotion
                score += Knight.KNIGHT_VALUE;
            }
            else if(moveType == 11 || moveType == 15){
                // queen promotion
                score += Queen.QUEEN_VALUE;
            }
            else{
                score += 300; // rook and bishop not as useful as Queen / Knight promotion (knight discovered checks)
            }
        }

        if(MoveGenerator.isCastling(move)){
            score += 100;
        }

        if(board.checkPawnAttacking(isWhitePiece, end, 0) != 0){
            // if tile being moved to is attacked by enemy pawn, usually bad.
            score -= startPiece.getValue();
        }

        return score;
    }
}
