public class Move {
    private final Board board;
    private final int startPosition;
    private final int endPosition;
    private Piece attackedPiece;

    public Move(Board board, int startPosition, int endPosition){
        this.board = board;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.attackedPiece = null;
    }

    // makes the move on the board
    public void makeMove(){
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        if(!startTile.isOccupied()){
            throw new IllegalArgumentException("Tile does not contain a piece to move.");
        }
        // move the piece from start to end tile
        // set the start tile to null
        if(endTile.isOccupied()){   // attacking an enemy piece
            attackedPiece = endTile.getPiece(); // store a copy of attacked piece to undo move afterwards
            board.killPiece(getEnd());
        }
        // check if the move is an enpassant capture
        else if(isEnpassantCapture()){
            // kill the enemy pawn
            int enpassantPawn = board.getEnpassantPawnPosition();
            attackedPiece = board.getTile(enpassantPawn).getPiece();
            board.killPiece(enpassantPawn);
        }

        if(startTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getEnd(), startTile.getPiece().isWhite());
        }

        // set piece on end tile to piece on start tile
        endTile.setPiece(startTile.getPiece());
        // remove piece on start tile
        startTile.setPiece(null);
        calculateAttackData();
    }

    // undo the move made on the board
    public void unMake(){
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        if(endTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getStart(), endTile.getPiece().isWhite());
        }

        // move the piece back from end to start tile
        startTile.setPiece(endTile.getPiece());

        if(attackedPiece != null){  // undo the attacking move
            board.getTile(attackedPiece.getPosition()).setPiece(attackedPiece);
            board.addPiece(attackedPiece, attackedPiece.getPosition());
        }
        else{   // remove piece on end tile after it has moved to start tile
            endTile.setPiece(null);
        }
        calculateAttackData();
    }

    /**
     * Checks move being made is an Enpassant capture
     * @return true if it is an enpassant move
     */
    private boolean isEnpassantCapture(){
        return board.getTile(getStart()).getPiece().toString().equals("P") && board.getEnpassant() == getEnd();
    }

    /**
     * Checks if the king movement is a castling move
     * @return true if king is making a castling move, else return false
     */
    private boolean isCastling(){
        if(board.isWhiteTurn()){
            return getStart() == 60 && (getEnd() == 62 || getEnd() == 58);
        }
        else{
            return getStart() == 4 && (getEnd() == 6 || getEnd() == 2);
        }
    }

    /**
     * Resets and calculates the attack maps for opponents after making a move
     */
    private void calculateAttackData(){
        // if white made a move, recalculate black's attack map
        board.resetAttackData(!board.isWhiteTurn());
        if(board.isWhiteTurn()){
            board.calcBlackAttackMap();
        }
        else{
            board.calcWhiteAttackMap();
        }
    }

    private int getStart(){
        return startPosition;
    }

    private int getEnd(){
        return endPosition;
    }
}
