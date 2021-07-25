import java.util.Random;

public class Zobrist {
    /*
     * Zobrist hashing is a hash function used to hash a board position of the chess board.
     * This allows for quick look-ups of the same position from a transposition table
     *
     * At the start of the program, for each 64 square, 12 random numbers will be calculated for each of the
     * 6 piece types for the 2 sides for a total of 64 * 12 keys.
     *
     * Piece indexing in hashKey array:
     *      0 - white pawn
     *      1 - white knight
     *      2 - white bishop
     *      3 - white rook
     *      4 - white queen
     *      5 - white king
     *      6 - black pawn
     *      7 - black knight
     *      8 - black bishop
     *      9 - black rook
     *      10 - black queen
     *      11 - black king
     */

    public static long[][] pieceKeys;

    static{
        // generate a random number for each piece type at each square
        pieceKeys = new long[64][12];
        Random rand = new Random();

        for(int i = 0; i < 64; i++){
            for(int j = 0; j < 12; j++){
                pieceKeys[i][j] = rand.nextLong();
            }
        }
    }

    public static long generateHash(Board board){
        long zobristHash = 0;
        PieceList whitePieces = board.getWhitePieces();
        PieceList blackPieces = board.getBlackPieces();

        Piece piece;
        for(int i = 0; i < whitePieces.getCount(); i++){
            piece = board.getTile(whitePieces.occupiedTiles[i]).getPiece();
            zobristHash ^= getKey(piece);
        }

        for(int i = 0; i < blackPieces.getCount(); i++){
            piece = board.getTile(blackPieces.occupiedTiles[i]).getPiece();
            zobristHash ^= getKey(piece);
        }

        return zobristHash;
    }

    public static long movePiece(long zobrist, int start, int end, Piece piece){
        int index = getPieceIndex(piece);
        zobrist ^= pieceKeys[start][index];
        zobrist ^= pieceKeys[end][index];
        return zobrist;
    }

    public static long update(long zobrist, int position, Piece piece){
        zobrist ^= pieceKeys[position][getPieceIndex(piece)];
        return zobrist;
    }

    public static long getKey(Piece piece){
        int pieceIndex = getPieceIndex(piece);
        return pieceKeys[piece.getPosition()][pieceIndex];
    }

    public static int getPieceIndex(Piece piece){
        if(piece.isWhite()){
            if(piece.isPawn()){
                return 0;
            }
            else if(piece.isKnight()){
                return 1;
            }
            else if(piece.isBishop()){
                return 2;
            }
            else if(piece.isRook()){
                return 3;
            }
            else if(piece.isQueen()){
                return 4;
            }
            else if(piece.isKing()){
                return 5;
            }
        }
        else{
            if(piece.isPawn()){
                return 6;
            }
            else if(piece.isKnight()){
                return 7;
            }
            else if(piece.isBishop()){
                return 8;
            }
            else if(piece.isRook()){
                return 9;
            }
            else if(piece.isQueen()){
                return 10;
            }
            else if(piece.isKing()){
                return 11;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        Board board = new Board();
        board.init(FENUtilities.startFEN);

        long start = System.nanoTime();
        long key = Zobrist.generateHash(board);
        long finish = System.nanoTime();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed;

        System.out.println("Zobrist Key: " + key);
        System.out.println("Time Taken: " + convertTime + " ns");
    }
}
