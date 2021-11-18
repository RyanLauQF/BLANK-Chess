import java.util.ArrayList;
import java.util.Comparator;

public class SEE {
    private final Board board;
    private final ArrayList<Piece> whiteAttackers;
    private final ArrayList<Piece> blackAttackers;

    public SEE(Board board){
        this.board = board;
        whiteAttackers = new ArrayList<>();
        blackAttackers = new ArrayList<>();
    }

    public int seeCapture(int from, int to){
        int value = 0;
        Piece capturedPiece, attackingPiece;
        attackingPiece = board.getTile(from).getPiece();
        if((capturedPiece = board.getTile(to).getPiece()) != null){
            value = capturedPiece.getPieceValue(); // 100
        }
        else{
            return value;
        }
        getAttackingPieces(to, attackingPiece);

        boolean isWhiteTurn = capturedPiece.isWhite();  // true
        ArrayList<Piece> attackerList;
        if(isWhiteTurn){
            attackerList = whiteAttackers;  //using this
        }
        else{
            attackerList = blackAttackers;
        }

        int attackerIndex = 0;

        while(!attackerList.isEmpty()){
            capturedPiece = attackingPiece; // captured piece set to black knight
            if(capturedPiece.isKing()){
                break;
            }
            if(capturedPiece.isWhite() != isWhiteTurn){
                value -= capturedPiece.getPieceValue(); // goes here 100 - 300 = -200
            }
            else{
                value += capturedPiece.getPieceValue();
            }

            attackingPiece = attackerList.remove(attackerIndex);    // attacking piece set to white pawn
            if(attackingPiece.isWhite()){
                attackerList = blackAttackers;
            }
            else{
                attackerList = whiteAttackers; //using this
            }
        }

        return value;
    }

    public void getAttackingPieces(int square, Piece attackingPiece){
        int end, offSet;

        int[] knightSquares = MoveDirections.knightOffSets;
        // search knight attacking squares
        for(int i = 0; i < 8; i++){
            end = square + knightSquares[i];
            // if it is a valid knight move
            if(Math.abs(board.getRow(square) - board.getRow(end)) + Math.abs(board.getCol(square) - board.getCol(end)) == 3
                    && end >= 0 && end < 64){
                if(board.getTile(end).isOccupied()){
                    Piece piece = board.getTile(end).getPiece();
                    if(piece.isKnight() && piece != attackingPiece){
                        if(piece.isWhite()){
                            whiteAttackers.add(piece);
                        }
                        else{
                            blackAttackers.add(piece);
                        }
                    }
                }
            }
        }

        int[] directions = MoveDirections.getDirections(square);
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++) {
                end = square + (offSet * (i + 1));
                if (board.getTile(end).isOccupied()) {
                    Piece piece = board.getTile(end).getPiece();
                    if (piece.isPawn() || piece.isKnight() || piece == attackingPiece) {
                        continue;
                    }
                    if(piece.isKing() && i == 0){
                        if(piece.isWhite()){
                            whiteAttackers.add(piece);
                        }
                        else{
                            blackAttackers.add(piece);
                        }
                        continue;
                    }
                    if (index < 4) {  // for straight directions, check if it is a rook / queen
                        if (piece.isRook() || piece.isQueen()) {
                            if(piece.isWhite()){
                                whiteAttackers.add(piece);
                            }
                            else{
                                blackAttackers.add(piece);
                            }
                        }
                    }
                    else {   // diagonal directions
                        if (piece.isBishop() || piece.isQueen()) {
                            if(piece.isWhite()){
                                whiteAttackers.add(piece);
                            }
                            else{
                                blackAttackers.add(piece);
                            }
                        }
                    }
                }
            }
        }

        // search for pawn attackers
        searchPawnAttackers(true, square, attackingPiece);  // search for black pawns
        searchPawnAttackers(false, square, attackingPiece); // search for white pawns

        // sort attackers using piece value from smallest to largest
        whiteAttackers.sort(Comparator.comparingInt(Piece::getPieceValue));
        blackAttackers.sort(Comparator.comparingInt(Piece::getPieceValue));
    }

    public void searchPawnAttackers(boolean isWhitePiece, int piecePosition, Piece attackingPiece){
        int rightPawnIndex;
        int leftPawnIndex;

        if(isWhitePiece){
            // set black pawn locations relative to white piece
            leftPawnIndex = piecePosition -9;
            rightPawnIndex = piecePosition -7;
        }
        else{
            // set white pawn location relative to black piece
            leftPawnIndex = piecePosition + 7;
            rightPawnIndex = piecePosition + 9;
        }

        boolean rightEdgePiece = false;
        boolean leftEdgePiece = false;

        if(piecePosition % 8 == 0){ // piece on left edge
            leftEdgePiece = true;
        }
        else if (piecePosition % 8 == 7){  // piece on right edge
            rightEdgePiece = true;
        }

        if(!rightEdgePiece && checkBound(rightPawnIndex) && board.getTile(rightPawnIndex).isOccupied()){
            Piece piece = board.getTile(rightPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhitePiece && piece != attackingPiece){
                if(piece.isWhite()){
                    whiteAttackers.add(piece);
                }
                else{
                    blackAttackers.add(piece);
                }
            }
        }
        if(!leftEdgePiece && checkBound(leftPawnIndex) && board.getTile(leftPawnIndex).isOccupied()){
            Piece piece = board.getTile(leftPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhitePiece && piece != attackingPiece){
                if(piece.isWhite()){
                    whiteAttackers.add(piece);
                }
                else{
                    blackAttackers.add(piece);
                }
            }
        }
    }

    private boolean checkBound(int index){
        return index >= 0 && index <= 63;
    }

    public static void main(String[] args) {
//        Board board = new Board();
//        board.init("8/8/1n2k3/2pP2r1/2K5/8/3Q2B1/8 b - - 0 1");
//
//        SEE test = new SEE(board);
//        System.out.println(test.seeCapture(17, 27));
    }
}
