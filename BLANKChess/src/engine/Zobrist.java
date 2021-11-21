import java.util.*;

public class Zobrist {
    /*
     * Zobrist hashing is a hash function used to hash a board position of the chess board.
     * This allows for quick look-ups of the same position from a transposition table
     *
     * At the start of the program:
     *      - For each 64 square, 12 random numbers will be calculated for each of the 6 piece types for the 2 sides
     *      - 4 numbers for castling rights
     *      - 8 numbers for enpassant available files
     *      - 1 number to check which sides turn.
     *
     * Total of 64 * 12 + 8 + 4 + 1 = 781 random numbers
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
     *
     * Castling rights indexing:
     *      0 - white king side castle
     *      1 - white queen side castle
     *      2 - black king side castle
     *      3 - black queen side castle
     *
     * Enpassant hash indexing:
     *      0 - a file
     *      1 - b file
     *      2 - c file
     *      3 - d file
     *      4 - e file
     *      5 - f file
     *      6 - g file
     *      7 - h file
     */

    public static long[][] pieceKeys;
    public static long isWhiteTurnHash;
    public static long[] castlingHash;
    public static long[] enpassantHash;


    static{
        // generate a random number for each piece type at each square
        pieceKeys = new long[64][12];
        castlingHash = new long[4];
        enpassantHash = new long[8];

        Random rand = new Random();
        rand.setSeed(7620876781721148884L);

        // generate piece position hash
        for(int i = 0; i < 64; i++){
            for(int j = 0; j < 12; j++){
                pieceKeys[i][j] = rand.nextLong();
            }
        }

        // generate turn hash
        isWhiteTurnHash = rand.nextLong();

        // generate castling rights hash
        for(int i = 0; i < 4; i++){
            castlingHash[i] = rand.nextLong();
        }

        // generate enpassant hash for each file
        for(int i = 0; i < 8; i++){
            enpassantHash[i] = rand.nextLong();
        }
    }

    /**
     * Generates a Zobrist hash which represents the board in its current position
     * @param board refers to the current state of the board to be hashed
     * @return the Zobrist hash of the board state
     */
    public static long generateHash(Board board){
        long zobristHash = 0;
        PieceList whitePieces = board.getWhitePieces();
        PieceList blackPieces = board.getBlackPieces();

        // get piece position data
        Piece piece;
        for(int i = 0; i < whitePieces.getCount(); i++){
            piece = board.getTile(whitePieces.occupiedTiles[i]).getPiece();
            zobristHash ^= getKey(piece);
        }

        for(int i = 0; i < blackPieces.getCount(); i++){
            piece = board.getTile(blackPieces.occupiedTiles[i]).getPiece();
            zobristHash ^= getKey(piece);
        }

        // get turn data
        if(board.isWhiteTurn()){
            zobristHash ^= isWhiteTurnHash;
        }

        // get castling data
        if(board.getWhiteKingSideCastle()){
            zobristHash ^= castlingHash[0];
        }

        if(board.getWhiteQueenSideCastle()){
            zobristHash ^= castlingHash[1];
        }

        if(board.getBlackKingSideCastle()){
            zobristHash ^= castlingHash[2];
        }

        if(board.getBlackQueenSideCastle()){
            zobristHash ^= castlingHash[3];
        }

        // get enpassant data
        if(board.canEnpassant()){
            zobristHash ^= enpassantHash[Piece.getCol(board.getEnpassant())];
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
