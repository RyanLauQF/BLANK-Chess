public class Move {
    private final Board board;
    private final int startPosition;
    private final int endPosition;
    private Piece attackedPiece;
    private boolean isEnpassant;
    private boolean rookLostCastling;
    private boolean kingLostCastling;
    private boolean pawnPromotion;

    private final int previousEnpassantPosition;
    private final boolean whiteKingSideCastling;
    private final boolean whiteQueenSideCastling;
    private final boolean blackKingSideCastling;
    private final boolean blackQueenSideCastling;

    /**
     * Move a piece on the board based on move rules and update the board
     * whilst checking if the game has ended.
     *
     * Information to be updated:
     *      1) Piece location from start to end tile (update location of alive piece when it moves)
     *      2) Castling rights
     *      3) Enpassant availability
     *      4) Pawn promotion if pawn reached last row (Select either Rook, Knight, Bishop or Queen)
     *      5) 50 move rule
     *      6) Change to opponent's turn
     *
     * @param board refers to a reference to the current state of the chess board
     * @param startPosition refers to the (starting) location of piece being moved
     * @param endPosition refers to the final location the piece will end up
     */
    public Move(Board board, int startPosition, int endPosition){
        this.board = board;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.attackedPiece = null;
        this.pawnPromotion = false;

        // to reset enpassant and castling rights during unMove
        this.previousEnpassantPosition = board.getEnpassant();
        this.isEnpassant = false;
        this.kingLostCastling = false;
        this.rookLostCastling = false;
        this.whiteKingSideCastling = board.getWhiteKingSideCastle();
        this.whiteQueenSideCastling = board.getWhiteQueenSideCastle();
        this.blackKingSideCastling = board.getBlackKingSideCastle();
        this.blackQueenSideCastling = board.getBlackQueenSideCastle();
    }

    public void makeMove(){
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        // Calculate if there is an enpassant availability if the move is a double pawn move
        int enpassantPosition = -1;
        if(isPawnDoubleMove()){
            enpassantPosition = (getStart() + getEnd()) / 2;
        }

        // if the move is a king moving
        if(startTile.getPiece().isKing()){
            // update king position on board
            board.setKingPosition(getEnd(), startTile.getPiece().isWhite());

            // check if it is a castling move and move the rook (update rook position)
            if(board.hasCastlingRights()) {
                // white castling
                if (startTile.getPiece().isWhite()) {
                    if (isKingSideCastling()) {
                        // shift white king side rook (from position 63 to 61) and remove castling rights
                        updatePiecePosition(63, 61);
                        board.getTile(61).setPiece(board.getTile(63).getPiece());
                        board.getTile(63).setPiece(null);
                    }
                    else if (isQueenSideCastling()) {
                        // shift white queen side rook (from position 56 to 59) and remove castling rights
                        updatePiecePosition(56, 59);
                        board.getTile(59).setPiece(board.getTile(56).getPiece());
                        board.getTile(56).setPiece(null);
                    }
                    // even if it is not a castling move, remove white castling rights as king has moved
                    board.setWhiteKingSideCastle(false);
                    board.setWhiteQueenSideCastle(false);
                }
                // black castling
                else {
                    if (isKingSideCastling()) {
                        // shift black king side rook (from position 7 to 5) and remove castling rights
                        updatePiecePosition(7, 5);
                        board.getTile(5).setPiece(board.getTile(7).getPiece());
                        board.getTile(7).setPiece(null);

                    } else if(isQueenSideCastling()) {
                        // shift black queen side rook (from position 0 to 3) and remove castling rights
                        updatePiecePosition(0, 3);
                        board.getTile(3).setPiece(board.getTile(0).getPiece());
                        board.getTile(0).setPiece(null);
                    }
                    // even if it is not a castling move, remove black castling rights as king has moved
                    board.setBlackKingSideCastle(false);
                    board.setBlackQueenSideCastle(false);
                }
                kingLostCastling = true;
            }
        }
        // if rook is moving, disable the rook side castling
        else if(startTile.getPiece().isRook()){
            // disable castling rights if a rook is moving
            if(board.hasCastlingRights()){
                board.setRookSideCastling(board.isWhiteTurn(), getStart(), false);
                rookLostCastling = true;
            }
        }

        // check move is a normal capture
        if(endTile.isOccupied()){   // attacking an enemy piece
            attackedPiece = endTile.getPiece(); // store a copy of attacked piece to undo move afterwards
            board.removePiece(getEnd());
        }
        // check move is an enpassant capture
        else if(isEnpassantCapture()){
            // kill the enemy pawn
            int enpassantPawnAttacked = board.getEnpassantPawnPosition();
            attackedPiece = board.getTile(enpassantPawnAttacked).getPiece();
            board.removePiece(enpassantPawnAttacked);
            board.getTile(enpassantPawnAttacked).setPiece(null);
            isEnpassant = true;
        }

        if(isPawnPromotion()){
            board.promote(Piece.PieceType.QUEEN, startTile);
            pawnPromotion = true;
        }

        // updates piece position in piece list in board which tracks individual pieces
        updatePiecePosition(getStart(), getEnd());

        // Shift the piece from end to start tile and set start tile to null
        endTile.setPiece(startTile.getPiece());
        startTile.setPiece(null);

        // Update enpassant availability after shifting the pieces
        board.setEnpassant(enpassantPosition);
        board.setTurn(!board.isWhiteTurn());
    }

    // undo the move made on the board
    public void unMake(){
        board.setTurn(!board.isWhiteTurn());
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());
        updatePiecePosition(getEnd(), getStart());

        if(endTile.getPiece().isKing()){    // if piece is a King
            // update king position on board
            board.setKingPosition(getStart(), endTile.getPiece().isWhite());

            if(kingLostCastling) {
                // undo castling move
                if (endTile.getPiece().isWhite()) {
                    if (isKingSideCastling()) {
                        // shift back white king side rook (from position 61 to 63) and remove castling rights
                        updatePiecePosition(61, 63);
                        board.getTile(63).setPiece(board.getTile(61).getPiece());
                        board.getTile(61).setPiece(null);
                    }
                    else if(isQueenSideCastling()){
                        // shift back white queen side rook (from position 59 to 56) and remove castling rights
                        updatePiecePosition(59, 56);
                        board.getTile(56).setPiece(board.getTile(59).getPiece());
                        board.getTile(59).setPiece(null);
                    }
                }
                else {
                    if (isKingSideCastling()) {
                        // shift back black king side rook (from position 5 to 7) and remove castling rights
                        updatePiecePosition(5, 7);
                        board.getTile(7).setPiece(board.getTile(5).getPiece());
                        board.getTile(5).setPiece(null);

                    } else if(isQueenSideCastling()) {
                        // shift back black queen side rook (from position 3 to 0) and remove castling rights
                        updatePiecePosition(3, 0);
                        board.getTile(0).setPiece(board.getTile(3).getPiece());
                        board.getTile(3).setPiece(null);
                    }
                }
                // reset the castling moves
                board.setWhiteKingSideCastle(whiteKingSideCastling);
                board.setWhiteQueenSideCastle(whiteQueenSideCastling);
                board.setBlackKingSideCastle(blackKingSideCastling);
                board.setBlackQueenSideCastle(blackQueenSideCastling);
            }
        }

        if(rookLostCastling){
            board.setRookSideCastling(board.isWhiteTurn(), getStart(), true);
        }

        // undo enpassant pawn
        if(isEnpassant){
            board.getTile(attackedPiece.getPosition()).setPiece(attackedPiece);
            board.addPiece(attackedPiece, attackedPiece.getPosition());
            attackedPiece = null;
        }

        // reset enpassant to initial value before move
        board.setEnpassant(previousEnpassantPosition);

        // move the piece back from end to start tile
        startTile.setPiece(endTile.getPiece());

        if(attackedPiece != null){  // undo the attacking move
            board.getTile(attackedPiece.getPosition()).setPiece(attackedPiece);
            board.addPiece(attackedPiece, attackedPiece.getPosition());
        }
        else{   // remove piece on end tile after it has moved to start tile
            endTile.setPiece(null);
        }

        if(pawnPromotion){
            // reset the piece back to a pawn
            Piece piece = startTile.getPiece();
            startTile.setPiece(new Pawn(piece.isWhite(), piece.getPosition(), board));
        }
    }

    /**
     * Updates the piece list that keeps track of the location of pieces for each side of the board
     * @param startPosition refers to the initial position the piece occupies
     * @param endPosition refers to the position which the piece has moved to
     */
    private void updatePiecePosition(int startPosition, int endPosition){
        if(board.getTile(startPosition).getPiece().isWhite()){
            // if it is a white piece moving, update white pieces list
            board.getWhitePieces().movePiece(startPosition, endPosition);
        }
        else{
            // if it is a black piece moving, update black pieces list
            board.getBlackPieces().movePiece(startPosition, endPosition);
        }
        board.getTile(startPosition).getPiece().setPosition(endPosition);
    }


    /**
     * Checks that move is a pawn doing a double jump from start position (To get possible enpassant position)
     * @return true if pawn is at starting position and makes a double jump
     */
    public boolean isPawnDoubleMove(){
        if(board.getTile(getStart()).isOccupied() &&
                board.getTile(getStart()).getPiece().isPawn()){
            return Math.abs(getStart() - getEnd()) == 16;
        }
        return false;
    }

    /**
     * Check pawn piece has reached end of the board allowing it to promote
     * @return true for pawn being at last row of either side of the board
     */
    public boolean isPawnPromotion(){
        if(board.getTile(getStart()).getPiece().isPawn()){
           int row = board.getRow(getEnd());
           return row == 0 || row == 7;
        }
        return false;
    }

    /**
     * Checks move being made is an Enpassant capture
     * @return true if it is an enpassant move
     */
    public boolean isEnpassantCapture(){
        return (board.getTile(getStart()).getPiece().isPawn()) && (board.getEnpassant() == getEnd());
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
