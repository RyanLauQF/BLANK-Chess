/**
 * Keeps track of the individual pieces of each side on the board within an array
 */
public class PieceList {
    private final int[] boardTiles;
    public final int[] occupiedTiles;

    // Tracks the current count of the number of pieces in the current array
    // Only indexes in the occupiedTiles array up to the piece count are considered,
    // the indexes after the count will be kept unused
    private int pieceCounter;

    public PieceList(){
        this.boardTiles = new int[64]; // 64 squares on the chess board
        this.occupiedTiles = new int[16]; // 16 maximum possible pieces for each side
        this.pieceCounter = 0;
    }

    /**
     * Creates a deep copy of pieceList
     * @param pieceList refers to the piece list to be copied
     */
    public PieceList(PieceList pieceList){
        this.boardTiles = pieceList.boardTiles.clone();
        this.occupiedTiles = pieceList.occupiedTiles.clone();
        this.pieceCounter = pieceList.pieceCounter;
    }

    public void addPiece(int position){
        // the piece counting will also act as an indexer to give each piece their key based on when they are added
        boardTiles[position] = pieceCounter;
        occupiedTiles[pieceCounter] = position;
        pieceCounter++;
    }

    public void removePiece(int position){
        // Gets the index of the piece to be removed
        int removedPieceIndex = boardTiles[position];
        // finds the last piece in the list indexed at pieceCounter - 1 and
        // swap its position to index of piece to be removed
        occupiedTiles[removedPieceIndex] = occupiedTiles[pieceCounter - 1];
        // since the last piece now has the index of the removed piece,
        // update the piece index at the piece's original position in boardTiles
        boardTiles[occupiedTiles[removedPieceIndex]] = removedPieceIndex;
        pieceCounter--;
    }

    public void movePiece(int startPosition, int endPosition){
        int pieceToMoveIndex = boardTiles[startPosition];
        boardTiles[endPosition] = pieceToMoveIndex;
        occupiedTiles[pieceToMoveIndex] = endPosition;
    }

    public int getCount(){
        return this.pieceCounter;
    }
}
