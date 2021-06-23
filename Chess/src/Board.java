import java.util.ArrayList;

public class Board {
    // turn-related data
    private boolean isWhiteTurn;
//    private int halfMoveClock;
//    private int fullMoveNum;

    // track position of alive pieces on the board for each side
    private final ArrayList<Integer> whitePieces;
    private final ArrayList<Integer> blackPieces;

    // used for fast checking if king is in check
    private int kingPosition;
    private boolean[] whiteAttackMap;
    private boolean[] blackAttackMap;

    // board is made up of 64 tile objects
    public Tile[] board;

    // Board constructor
    public Board(){
        board = new Tile[64];
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();
    }

    // initiate board data from FEN
    public void init(String FEN){
        // set board to default position / custom FEN position
        FENUtilities.convertFENtoBoard(FEN, this);

        // initiate attack maps for white and black side using a bitset
        whiteAttackMap = new boolean[64];
        blackAttackMap = new boolean[64];
        initAttackData();
    }

    // initiate data for attack maps
    public void initAttackData(){
        // get all squares which white pieces are attacking and set to true on bitset
        for(int each : getWhitePieces()){
            for(int moves : board[each].getPiece().getDefendingSquares()){
                setWhiteAttackMap(moves, true);
            }
        }

        // get all squares which black pieces are attacking and set to true on bitset
        for(int each : getBlackPieces()){
            for(int moves : board[each].getPiece().getDefendingSquares()){
                setBlackAttackMap(moves, true);
            }
        }
    }

    // returns a list of possible positions of all legal moves on the board
    public ArrayList<Integer> getLegalMoves(){
        ArrayList <Integer> moveList = new ArrayList<>();
        // *pseudo-legal moves are filtered out when generating moves from individual pieces
        if (isWhiteTurn()){  // get all white legal moves
            for(int each : getWhitePieces()){ // goes through all white pieces
                moveList.addAll(board[each].getPiece().getLegalMoves()); // merge list
            }
        }
        else{   // get all black legal moves
            for(int each : getBlackPieces()){ // goes through all white pieces
                moveList.addAll(board[each].getPiece().getLegalMoves()); // merge list
            }
        }
        return moveList;
    }

    // check attack map of opposing team to see if king is being attacked by any opposing piece
    public boolean isKingChecked(){
        if(isWhiteTurn()){
            return blackAttackMap[getKingPosition()];
        }
        else{
            return whiteAttackMap[getKingPosition()];
        }
    }

    // take the piece off the board once killed
    public void killPiece(int position){
        if(board[position].getPiece().isWhite()){
            whitePieces.remove(position);
        }
        else{
            blackPieces.remove(position);
        }
    }

//    public void addPiece(){
//
//    }

    /*** GETTER FUNCTIONS ***/
    public ArrayList<Integer> getWhitePieces(){
        return whitePieces;
    }

    public ArrayList<Integer> getBlackPieces(){
        return blackPieces;
    }

    public boolean isWhiteTurn(){
        return isWhiteTurn;
    }

    public Tile[] getBoard(){
        return board;
    }

    public Tile getTile(int position){
        return board[position];
    }

    public int getKingPosition() {
        return kingPosition;
    }

    /*** SET FUNCTIONS ***/
    public void setTurn(boolean whiteTurn){
        this.isWhiteTurn = whiteTurn;
    }

    public void setKingPosition(int kingPosition) {
        this.kingPosition = kingPosition;
    }

    public void setWhiteAttackMap(int bitIndex, boolean isTrue){
        whiteAttackMap[bitIndex] = isTrue;
    }

    public void setBlackAttackMap(int bitIndex, boolean isTrue){
        blackAttackMap[bitIndex] = isTrue;
    }

//    public void setHalfMoveClock(int halfMoveClock){
//        this.halfMoveClock = halfMoveClock;
//    }
//
//    public void setFullMoveNum(int fullMoveNum){
//        this.fullMoveNum = fullMoveNum;
//    }

    public void state(){    // print board
        for(int i = 0; i < 8; i++){
            for(int k = 0; k < 8; k++){
                if(board[k + (i * 8)].getPiece() == null){
                    System.out.print("- ");
                }

                else{
                    if(board[k + (i * 8)].getPiece().isWhite()){  // white == true, upper case
                        System.out.print(board[k + (i * 8)].getPiece().toString().toUpperCase() + " ");
                    }
                    else{
                        System.out.print(board[k + (i * 8)].getPiece().toString().toLowerCase() + " ");
                    }
                }
            }
            System.out.print("       ");
            for(int k = 0; k < 8; k++){
                int a = k + (i * 8);
                System.out.print(String.format("%02d", a) + " ");
            }
            System.out.println();
        }
    }
    public static void main(String[] args){
        Board b = new Board();
        String FEN = "8/8/8/6k1/1r6/r7/8/K7 w - - 0 1";
        b.init(FENUtilities.startFEN);

        b.state();
        System.out.println();
        System.out.print("Moves: ");
        if (b.isWhiteTurn()) System.out.println("(White)");
        else System.out.println("(Black)");

        int counter = 0;
        for(int move : b.getLegalMoves()){
            System.out.print(move + " ");
            counter++;
        }
        System.out.println();

        System.out.println("Count: " + counter);
        System.out.println("King position: " + b.getKingPosition());
        System.out.println("FEN: " + FEN);
    }
}
