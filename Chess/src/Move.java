public class Move {
    private final Board board;
    private final int startPosition;
    private final int endPosition;
    private Piece attackedPiece;
    private boolean isEnpassant;

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
            System.out.println(startTile.getPosition());
            throw new IllegalArgumentException("Tile does not contain a piece to move.");
        }
        updatePiecePosition(getStart(), getEnd());
        // move the piece from start to end tile
        // set the start tile to null
        if(endTile.isOccupied()){   // attacking an enemy piece
            attackedPiece = endTile.getPiece(); // store a copy of attacked piece to undo move afterwards
            board.removePiece(getEnd());
        }
        // check if the move is an enpassant capture
        else if(isEnpassantCapture()){
            // kill the enemy pawn
            int enpassantPawnAttacked = board.getEnpassantPawnPosition();
            attackedPiece = board.getTile(enpassantPawnAttacked).getPiece();
            board.removePiece(enpassantPawnAttacked);
            board.getTile(enpassantPawnAttacked).setPiece(null);
            isEnpassant = true;
        }

        if(startTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getEnd(), startTile.getPiece().isWhite());
            // check if it is a castling move and move the rook
            if(isKingSideCastling()){
                if(startTile.getPiece().isWhite()){
                    // shift white king side rook (from position 63 to 61) and remove castling rights
                    board.getTile(61).setPiece(board.getTile(63).getPiece());
                    board.getTile(63).setPiece(null);
                    board.setWhiteKingSideCastle(false);
                }
                else{
                    // shift black king side rook (from position 7 to 5) and remove castling rights
                    board.getTile(5).setPiece(board.getTile(7).getPiece());
                    board.getTile(7).setPiece(null);
                    board.setBlackKingSideCastle(false);
                }
            }
            else if(isQueenSideCastling()){
                if(startTile.getPiece().isWhite()){
                    // shift white queen side rook (from position 56 to 69) and remove castling rights
                    board.getTile(59).setPiece(board.getTile(56).getPiece());
                    board.getTile(56).setPiece(null);
                    board.setWhiteQueenSideCastle(false);
                }
                else{
                    // shift black queen side rook (from position 0 to 3) and remove castling rights
                    board.getTile(3).setPiece(board.getTile(0).getPiece());
                    board.getTile(0).setPiece(null);
                    board.setBlackQueenSideCastle(false);
                }
            }
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
        updatePiecePosition(getEnd(), getStart());

        if(endTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getStart(), endTile.getPiece().isWhite());

            // undo castling move
            if(isKingSideCastling()){
                if(endTile.getPiece().isWhite()){
                    // shift back white king side rook (from position 61 to 63) and remove castling rights
                    board.getTile(63).setPiece(board.getTile(61).getPiece());
                    board.getTile(61).setPiece(null);
                    board.setWhiteKingSideCastle(true);
                }
                else{
                    // shift back black king side rook (from position 5 to 7) and remove castling rights
                    board.getTile(7).setPiece(board.getTile(5).getPiece());
                    board.getTile(5).setPiece(null);
                    board.setBlackKingSideCastle(true);
                }
            }
            else if(isQueenSideCastling()){
                if(endTile.getPiece().isWhite()){
                    // shift back white queen side rook (from position 59 to 56) and remove castling rights
                    board.getTile(56).setPiece(board.getTile(59).getPiece());
                    board.getTile(59).setPiece(null);
                    board.setWhiteQueenSideCastle(true);
                }
                else{
                    // shift back black queen side rook (from position 3 to 0) and remove castling rights
                    board.getTile(0).setPiece(board.getTile(3).getPiece());
                    board.getTile(3).setPiece(null);
                    board.setBlackQueenSideCastle(true);
                }
            }
        }
        // undo enpassant pawn
        if(isEnpassant){
            board.getTile(attackedPiece.getPosition()).setPiece(attackedPiece);
            board.addPiece(attackedPiece, attackedPiece.getPosition());
            attackedPiece = null;
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
     * Updates the piece Hashset that keeps track of the location of pieces for each side of the board
     * @param startPosition refers to the initial position the piece occupies
     * @param endPosition refers to the position which the piece has moved to
     */
    private void updatePiecePosition(Integer startPosition, int endPosition){
        if(board.getTile(startPosition).getPiece().isWhite()){
            // if it is a white piece moving, update white pieces hashset
            board.getWhitePieces().remove(startPosition);
            board.getWhitePieces().add(endPosition);
        }
        else{
            // if it is a black piece moving, update black pieces hashset
            board.getBlackPieces().remove(startPosition);
            board.getBlackPieces().add(endPosition);
        }
        board.getTile(startPosition).getPiece().setPosition(endPosition);
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

    /**
     * Checks that move is a pawn doing a double jump from start position (To get possible enpassant position)
     * @return true if pawn is at starting position and makes a double jump
     */
    public boolean isPawnDoubleMove(){
        if(board.getTile(getStart()).isOccupied() &&
                board.getTile(getStart()).getPiece().toString().equals("P")){
            return Math.abs(getStart() - getEnd()) == 16;
        }
        return false;
    }

    /**
     * Check pawn piece has reached end of the board allowing it to promote
     * @return true for pawn being at last row of either side of the board
     */
    public boolean isPawnPromotion(){
        if(board.getTile(getStart()).getPiece().toString().equals("P")){
           int row = board.getRow(getEnd());
           return row == 0 || row == 7;
        }
        return false;
    }

    /**
     * Checks move being made is an Enpassant capture
     * @return true if it is an enpassant move
     */
    private boolean isEnpassantCapture(){
        return board.getTile(getStart()).getPiece().toString().equals("P") && board.getEnpassant() == getEnd();
    }

    /**
     * Checks king movement is a king side castling move
     * @return true if king is making a castling move, else return false
     */
    private boolean isKingSideCastling(){
        if(board.isWhiteTurn()){
            return getStart() == 60 && getEnd() == 62;
        }
        else{
            return getStart() == 4 && getEnd() == 6;
        }
    }

    /**
     * Checks king movement is a queen side castling move
     * @return true if king is making a castling move, else return false
     */
    private boolean isQueenSideCastling(){
        if(board.isWhiteTurn()){
            return getStart() == 60 && getEnd() == 58;
        }
        else{
            return getStart() == 4 && getEnd() == 2;
        }
    }

    private int getStart(){
        return startPosition;
    }

    private int getEnd(){
        return endPosition;
    }
}
