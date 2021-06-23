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
        }
        if(startTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getEnd());
        }
        // set piece on end tile to piece on start tile
        endTile.setPiece(startTile.getPiece());
        // remove piece on start tile
        startTile.setPiece(null);
    }

    // undo the move made on the board
    public void unMake(){
        Tile startTile = board.getTile(getStart());
        Tile endTile = board.getTile(getEnd());

        if(endTile.getPiece().toString().equals("K")){    // if piece is a King
            // update king position on board
            board.setKingPosition(getStart());
        }

        // move the piece back from end to start tile
        startTile.setPiece(endTile.getPiece());

        if(attackedPiece != null){  // undo the attacking move
            endTile.setPiece(attackedPiece);
        }
        else{   // remove piece on end tile after it has moved to start tile
            endTile.setPiece(null);
        }
    }

    private int getStart(){
        return startPosition;
    }

    private int getEnd(){
        return endPosition;
    }
}
