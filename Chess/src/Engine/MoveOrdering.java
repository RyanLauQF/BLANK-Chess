import java.util.ArrayList;

public class MoveOrdering {
    public static ArrayList<Short> orderMoves(ArrayList<Short> moves, Board board) {
        moves.sort((move1, move2) -> {
            int moveScore1 = getMoveScore(move1, board);
            int moveScore2 = getMoveScore(move2, board);

            return Integer.compare(moveScore1, moveScore2);
        });
        return moves;
    }

    private static int getMoveScore(Short move, Board board){
        // evaluate the move scores
        int score = 0;
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);

        boolean isWhitePiece = board.getTile(start).getPiece().isWhite();
        Piece startPiece = board.getTile(start).getPiece();

        if(MoveGenerator.isCapture(move)){
            if (MoveGenerator.getMoveType(move) == 4) {
                // normal capture (exclude enpassant)
                score += (10 * (board.getTile(end).getPiece().getValue() - startPiece.getValue()));
            }
        }
        if(MoveGenerator.isPromotion(move)){
            int moveType = MoveGenerator.getMoveType(move);
            if(moveType == 8 || moveType == 12){
                // knight promotion
                score += 320;
            }
            else if(moveType == 11 || moveType == 15){
                // queen promotion
                score += 900;
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

    public static ArrayList<Short> quiescenceOrdering(ArrayList<Short> moves, Board board) {
        ArrayList<Short> captureMoves = new ArrayList<>();
        for(Short move : moves){
            if(MoveGenerator.isCapture(move)){
                captureMoves.add(move);
            }
        }
        captureMoves.sort((move1, move2) -> {
            int moveScore1 = getMoveScore(move1, board);
            int moveScore2 = getMoveScore(move2, board);

            return Integer.compare(moveScore1, moveScore2);
        });
//        moves.sort((move1, move2) -> {
//            int moveScore1 = getQuiescenceScore(move1, board);
//            int moveScore2 = getQuiescenceScore(move2, board);
//
//            return Integer.compare(moveScore1, moveScore2);
//        });
        return captureMoves;
    }

    private static int getQuiescenceScore(Short move, Board board){
        int moveType = MoveGenerator.getMoveType(move);
        int score = 0;
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);
        Piece startPiece = board.getTile(start).getPiece();

        if(MoveGenerator.isCapture(move)){
            if (moveType == 4) {
                // normal capture (exclude enpassant)
                score += (10 * (board.getTile(end).getPiece().getValue() - startPiece.getValue()));
            }
            score += 10000;  // prioritise captures for quiescence search
        }
        if(MoveGenerator.isPromotion(move)){
            if(moveType == 8 || moveType == 12){
                // knight promotion
                score += 320;
            }
            else if(moveType == 11 || moveType == 15){
                // queen promotion
                score += 900;
            }
            else{
                score += 300; // rook and bishop not as useful as Queen / Knight promotion (knight discovered checks)
            }
        }
        return score;
    }
}
