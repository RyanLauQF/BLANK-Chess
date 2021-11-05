public class Move {
    private final Board board;
    private final short encodedMove;
    private final int startPosition;
    private final int endPosition;
    public final int moveType;
    private Piece attackedPiece;
    private boolean isEnpassant;
    private boolean rookLostCastling;
    private boolean kingLostCastling;
    private final boolean pawnPromotion;

    private final int previousEnpassantPosition;
    private final boolean whiteKingSideCastling;
    private final boolean whiteQueenSideCastling;
    private final boolean blackKingSideCastling;
    private final boolean blackQueenSideCastling;
    private final Move previousMove;
    private final long zobrist;
    private final int fullMoveNumber;

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
     * @param encodedMove refers to the encoded move information containing start, end positions and move type
     */
    public Move(Board board, short encodedMove){
        this.board = board;
        this.encodedMove = encodedMove;
        this.startPosition = MoveGenerator.getStart(encodedMove);
        this.endPosition = MoveGenerator.getEnd(encodedMove);
        this.moveType = MoveGenerator.getMoveType(encodedMove);
        this.pawnPromotion = MoveGenerator.isPromotion(encodedMove);
        this.attackedPiece = null;

        // to reset enpassant and castling rights during unMove
        this.previousEnpassantPosition = board.getEnpassant();
        this.isEnpassant = false;
        this.kingLostCastling = false;
        this.rookLostCastling = false;
        this.whiteKingSideCastling = board.getWhiteKingSideCastle();
        this.whiteQueenSideCastling = board.getWhiteQueenSideCastle();
        this.blackKingSideCastling = board.getBlackKingSideCastle();
        this.blackQueenSideCastling = board.getBlackQueenSideCastle();

        this.previousMove = board.getPreviousMove();
        this.zobrist = board.getZobristHash();
        this.fullMoveNumber = board.getFullMoveNum();
    }

    public void makeMove(){
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        Piece startPiece = startTile.getPiece();
        boolean isWhitePiece = startPiece.isWhite();

        // increment full move counter in board after black moves
        if(!isWhitePiece){
            int fullMoveNum = board.getFullMoveNum();
            board.setFullMoveNum(++fullMoveNum);
        }

        // Calculate if there is an enpassant availability if the move is a double pawn move
        int enpassantPosition = -1;
        if(isPawnDoubleMove()){
            enpassantPosition = (getStart() + getEnd()) / 2;
        }

        // if the move is a king moving
        if(startPiece.isKing()){
            // update king position on board
            board.setKingPosition(getEnd(), isWhitePiece);

            // check if it is a castling move and move the rook (update rook position)
            if(board.hasCastlingRights()) {
                // white castling
                if (isWhitePiece) {
                    if (isKingSideCastling()) {
                        // shift white king side rook (from position 63 to 61) and remove castling rights
                        board.setHasKingCastled(true, true);
                        updatePiecePosition(true, 63, 61);
                        board.getTile(61).setPiece(board.getTile(63).getPiece());
                        board.getTile(63).setPiece(null);

                        // update white king side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[0]));
                    }
                    else if (isQueenSideCastling()) {
                        // shift white queen side rook (from position 56 to 59) and remove castling rights
                        board.setHasKingCastled(true, true);
                        updatePiecePosition(true, 56, 59);
                        board.getTile(59).setPiece(board.getTile(56).getPiece());
                        board.getTile(56).setPiece(null);

                        // update white queen side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[1]));
                    }
                    // even if it is not a castling move, remove white castling rights as king has moved
                    board.setWhiteKingSideCastle(false);
                    board.setWhiteQueenSideCastle(false);
                }
                // black castling
                else {
                    if (isKingSideCastling()) {
                        // shift black king side rook (from position 7 to 5) and remove castling rights
                        board.setHasKingCastled(true, false);
                        updatePiecePosition(false, 7, 5);
                        board.getTile(5).setPiece(board.getTile(7).getPiece());
                        board.getTile(7).setPiece(null);

                        // update black king side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[2]));

                    } else if(isQueenSideCastling()) {
                        // shift black queen side rook (from position 0 to 3) and remove castling rights
                        board.setHasKingCastled(true, false);
                        updatePiecePosition(false, 0, 3);
                        board.getTile(3).setPiece(board.getTile(0).getPiece());
                        board.getTile(0).setPiece(null);

                        // update black queen side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[3]));
                    }
                    // even if it is not a castling move, remove black castling rights as king has moved
                    board.setBlackKingSideCastle(false);
                    board.setBlackQueenSideCastle(false);
                }
                kingLostCastling = true;
            }
        }
        // if rook is moving, disable the rook side castling
        else if(startPiece.isRook()){
            // disable castling rights if a rook is moving
            if(board.hasCastlingRights()) {
                if (board.hasKingSideCastling(isWhitePiece) && isKingSideRook(isWhitePiece, getStart())) {
                    board.setRookSideCastling(isWhitePiece, getStart(), false);
                    rookLostCastling = true;

                    if(isWhitePiece){
                        // update white king side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[0]));
                    }
                    else{
                        // update black king side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[2]));
                    }
                }
                else if (board.hasQueenSideCastling(isWhitePiece) && isQueenSideRook(isWhitePiece, getStart())) {
                    board.setRookSideCastling(isWhitePiece, getStart(), false);
                    rookLostCastling = true;

                    if(isWhitePiece){
                        // update white queen side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[1]));
                    }
                    else{
                        // update black queen side castling hash
                        board.setZobristHash((board.getZobristHash() ^ Zobrist.castlingHash[3]));
                    }
                }
            }
        }

        // check move is a normal capture
        if(endTile.isOccupied()){   // attacking an enemy piece
            attackedPiece = endTile.getPiece(); // store a copy of attacked piece to undo move afterwards
            if(attackedPiece.isRook() && (isKingSideRook(attackedPiece.isWhite(), getEnd()) || isQueenSideRook(attackedPiece.isWhite(), getEnd()))){
                board.setRookSideCastling(attackedPiece.isWhite(), getEnd(), false);
                rookLostCastling = true;
            }
            board.removePiece(getEnd());

            // remove attacked piece from zobrist
            board.setZobristHash(Zobrist.update(board.getZobristHash(), getEnd(), attackedPiece));
        }
        // check move is an enpassant capture
        else if(isEnpassantCapture()){
            // kill the enemy pawn
            int enpassantPawnAttacked = board.getEnpassantPawnPosition();
            attackedPiece = board.getTile(enpassantPawnAttacked).getPiece();
            board.removePiece(enpassantPawnAttacked);
            board.getTile(enpassantPawnAttacked).setPiece(null);
            isEnpassant = true;

            // remove attacked pawn from zobrist
            board.setZobristHash(Zobrist.update(board.getZobristHash(), enpassantPawnAttacked, attackedPiece));
        }

        if(isPawnPromotion()){
            // remove pawn from zobrist
            board.setZobristHash(Zobrist.update(board.getZobristHash(), getStart(), startPiece));

            // pawn is promoted then moved to the end
            board.promote(getPromotionPieceType(moveType), startTile);

            // add promoted piece to zobrist
            board.setZobristHash(Zobrist.update(board.getZobristHash(), getStart(), startPiece));
        }

        // updates piece position in piece list in board which tracks individual pieces
        updatePiecePosition(board.isWhiteTurn(), getStart(), getEnd());

        // Shift the piece from start to end tile and set start tile to null
        endTile.setPiece(startTile.getPiece());
        startTile.setPiece(null);

        // Update enpassant availability after shifting the pieces
        board.setEnpassant(enpassantPosition);
        board.setPreviousMove(this);
        board.setTurn(!board.isWhiteTurn());
    }

    // undo the move made on the board
    public void unMake(){
        // return the full move number to original
        board.setFullMoveNum(fullMoveNumber);

        board.setTurn(!board.isWhiteTurn());
        board.setPreviousMove(previousMove);
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        updatePiecePosition(board.isWhiteTurn(), getEnd(), getStart());

        Piece endPiece = endTile.getPiece();
        boolean isWhitePiece = endPiece.isWhite();

        if(endPiece.isKing()){    // if piece is a King
            // update king position on board
            board.setKingPosition(getStart(), isWhitePiece);

            if(kingLostCastling) {
                // undo castling move
                if (isWhitePiece) {
                    if (isKingSideCastling()) {
                        board.setHasKingCastled(false, true);
                        // shift back white king side rook (from position 61 to 63) and remove castling rights
                        updatePiecePosition(true, 61, 63);
                        board.getTile(63).setPiece(board.getTile(61).getPiece());
                        board.getTile(61).setPiece(null);
                    }
                    else if(isQueenSideCastling()){
                        board.setHasKingCastled(false, true);
                        // shift back white queen side rook (from position 59 to 56) and remove castling rights
                        updatePiecePosition(true, 59, 56);
                        board.getTile(56).setPiece(board.getTile(59).getPiece());
                        board.getTile(59).setPiece(null);
                    }
                }
                else {
                    if (isKingSideCastling()) {
                        board.setHasKingCastled(false, false);
                        // shift back black king side rook (from position 5 to 7) and remove castling rights
                        updatePiecePosition(false, 5, 7);
                        board.getTile(7).setPiece(board.getTile(5).getPiece());
                        board.getTile(5).setPiece(null);

                    } else if(isQueenSideCastling()) {
                        board.setHasKingCastled(false, false);
                        // shift back black queen side rook (from position 3 to 0) and remove castling rights
                        updatePiecePosition(false, 3, 0);
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
        else if(endPiece.isRook()){
            board.setRookSideCastling(isWhitePiece, getStart(), rookLostCastling);
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
        startTile.setPiece(endPiece);

        if(attackedPiece != null){  // undo the attacking move
            if(attackedPiece.isRook()){
                board.setWhiteKingSideCastle(whiteKingSideCastling);
                board.setWhiteQueenSideCastle(whiteQueenSideCastling);
                board.setBlackKingSideCastle(blackKingSideCastling);
                board.setBlackQueenSideCastle(blackQueenSideCastling);
            }
            board.getTile(getEnd()).setPiece(attackedPiece);
            board.addPiece(attackedPiece, getEnd());
        }
        else{   // remove piece on end tile after it has moved to start tile
            endTile.setPiece(null);
        }

        if(isPawnPromotion()){
            // reset the piece back to a pawn
            Piece piece = startTile.getPiece();
            startTile.setPiece(new Pawn(piece.isWhite(), piece.getPosition(), board));
        }

        // reset zobrist hash back to original
        board.setZobristHash(zobrist);
    }

    /**
     * Makes a null move on the board for null move pruning
     */
    public void makeNullMove(){
        // set enpassant to unavailable
        int enpassantPosition = -1;
        board.setEnpassant(enpassantPosition);

        // set previous move to this move
        board.setPreviousMove(this);
        board.setTurn(!board.isWhiteTurn());
    }

    /**
     * Unmakes a null move on the board for null move pruning
     */
    public void unmakeNullMove(){
        // change back the turn
        board.setTurn(!board.isWhiteTurn());

        // set previous move back
        board.setPreviousMove(board.getPreviousMove());

        // reset enpassant to initial value before move
        board.setEnpassant(previousEnpassantPosition);
    }

    /**
     * Updates the piece list that keeps track of the location of pieces for each side of the board
     * @param isWhitePiece refers to the colour of the piece being moved
     * @param startPosition refers to the initial position the piece occupies
     * @param endPosition refers to the position which the piece has moved to
     */
    private void updatePiecePosition(boolean isWhitePiece, int startPosition, int endPosition){
        Piece piece = board.getTile(startPosition).getPiece();
        board.getPieceList(isWhitePiece).movePiece(startPosition, endPosition);
        piece.setPosition(endPosition);

        // update zobrist key in board
        board.setZobristHash(Zobrist.movePiece(board.getZobristHash(), startPosition, endPosition, piece));
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

    private boolean isPawnPromotion(){
        return this.pawnPromotion;
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

    private boolean isKingSideRook(boolean isWhiteRook, int position){
        if(isWhiteRook){
            return position == 63;
        }
        else{
            return position == 7;
        }
    }

    private boolean isQueenSideRook(boolean isWhiteRook, int position){
        if(isWhiteRook){
            return position == 56;
        }
        else{
            return position == 0;
        }
    }

    private Piece.PieceType getPromotionPieceType(int moveType){
        if(moveType == 8 || moveType == 12){
            return Piece.PieceType.KNIGHT;
        }
        else if(moveType == 9 || moveType == 13){
            return Piece.PieceType.BISHOP;
        }
        else if(moveType == 10 || moveType == 14){
            return Piece.PieceType.ROOK;
        }
        else if(moveType == 11 || moveType == 15){
            return Piece.PieceType.QUEEN;
        }
        return null;
    }

    public short getEncodedMove(){
        return encodedMove;
    }

    private int getStart(){
        return startPosition;
    }

    private int getEnd(){
        return endPosition;
    }
}
