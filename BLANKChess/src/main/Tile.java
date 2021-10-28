public class Tile {
    private Piece piece;

    // tile constructor
    public Tile(Piece piece){
        this.piece = piece;
    }

    // returns Piece on tile
    public Piece getPiece(){
        return this.piece;
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
