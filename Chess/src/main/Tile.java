public class Tile {
    private Piece piece;
    private final int position;

    // tile constructor
    public Tile(Piece piece, int position){
        this.piece = piece;
        this.position = position;
    }

    // returns Piece on tile
    public Piece getPiece(){
        return this.piece;
    }

    // returns position of tile on board
    public int getPosition(){
        return this.position;
    }

    // checks if there is a piece on the tile
    public boolean isOccupied(){
        return this.piece != null;
    }

    // set piece on tile
    public void setPiece(Piece p){
        this.piece = p;
    }
}
