import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;

public class Board {
    // turn-related data
    private boolean isWhiteTurn;
    private int halfMoveClock;
    private int fullMoveNum;

    // board is made up of 64 tile objects
    private final Tile[] board;

    // Castling rights for each side
    private boolean whiteKingSideCastle;
    private boolean whiteQueenSideCastle;
    private boolean blackKingSideCastle;
    private boolean blackQueenSideCastle;

    // Enpassant move possibility
    private int enpassantPosition;  // if no Enpassant move is available, initiate to -1

    // track position of alive pieces on the board for each side
    private final ArrayList<Integer> whitePieces;
    private final ArrayList<Integer> blackPieces;

    // used for fast checking if king is in check
    private int whiteKingPosition;
    private int blackKingPosition;
    private boolean[] whiteAttackMap;
    private boolean[] blackAttackMap;

    // used to reset attack data without reinitialising entire boolean array
    private Stack<Integer> trackWhiteMap;
    private Stack<Integer> trackBlackMap;

    /**
     * Board constructor
     */
    public Board(){
        board = new Tile[64];
        // initiate array list to keep track of position of all pieces on the board for each side
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();
    }

    /**
     *  Takes in a FEN (Forsyth–Edwards Notation) and converts the data onto the chess board
     *  Initiates attack data for both side pieces
     * @param FEN refers to a standard chess notation of a particular position
     */
    public void init(String FEN){
        // set board to default position / custom FEN position
        FENUtilities.convertFENtoBoard(FEN, this);
       // initiate attack data for both sides
        initAttackData();
    }

    /**
     * Initiates data for attack maps (track squares which each piece is defending)
     */
    public void initAttackData(){
        // initiate attack maps for white and black side using a boolean array
        whiteAttackMap = new boolean[64];
        blackAttackMap = new boolean[64];
        trackWhiteMap = new Stack<>();
        trackBlackMap = new Stack<>();

        calcWhiteAttackMap();
        calcBlackAttackMap();
    }

    public void calcWhiteAttackMap(){
        // get all squares which white pieces are attacking and set to true
        for(int each : getWhitePieces()){
            if(board[each].getPiece().toString().equals("P")){ // if it is a pawn, get its control squares using another method
                for(int pawnSquares : pawnDefendingSquares(each)){
                    setWhiteAttackMap(pawnSquares, true);
                    trackWhiteMap.push(pawnSquares);
                }
            }
            else{
                for(int moves : board[each].getPiece().getDefendingSquares()){
                    setWhiteAttackMap(moves, true);
                    trackWhiteMap.push(moves);
                }
            }
        }
    }

    public void calcBlackAttackMap(){
        // get all squares which black pieces are attacking and set to true
        for(int each : getBlackPieces()){
            if(board[each].getPiece().toString().equals("P")){ // if it is a pawn, get its control squares using another method
                for(int pawnSquares : pawnDefendingSquares(each)){
                    setBlackAttackMap(pawnSquares, true);
                    trackBlackMap.push(pawnSquares);
                }
            }
            else{
                for(int moves : board[each].getPiece().getDefendingSquares()){
                    setBlackAttackMap(moves, true);
                    trackBlackMap.push(moves);
                }
            }
        }
    }

    /**
     * Resets attack map to be used again
     * @param resetWhiteMap refers to the side which attack map is to be updated
     */
    public void resetAttackData(boolean resetWhiteMap){
        if(resetWhiteMap){  // reset white attack map
            while (!trackWhiteMap.isEmpty()) {
                setWhiteAttackMap(trackWhiteMap.pop(), false);
            }
        }
        else{   // reset black attack map
            while (!trackBlackMap.isEmpty()) {
                setBlackAttackMap(trackBlackMap.pop(), false);
            }
        }
    }

    /**
     * Gets all legal moves on the board for the current side's turn
     * E.g. if its white's turn, get all white legal moves.
     * @return list of positions of all legal moves on the board
     */
    public ArrayList<Integer> getAllLegalMoves(){
        ArrayList <Integer> moveList = new ArrayList<>();
        // pseudo-legal moves are filtered out when generating moves from individual pieces
        if (isWhiteTurn()){  // get all white legal moves
            Integer[] arr = new Integer[getWhitePieces().size()];
            getWhitePieces().toArray(arr);
            for(int each : arr){ // goes through all white pieces
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

    /**
     * Move a piece on the board based on move rules and update the board
     * whilst checking if the game has ended.
     *
     * Information to be updated:
     *      1) Piece location from start to end tile (update location of alive piece when it moves)
     *      2) Castling rights (is updated when makeMove() is called)
     *      3) Enpassant availability
     *      4) Pawn promotion if pawn reached last row (Select either Rook, Knight, Bishop or Queen)
     *      5) 50 move rule
     *      6) Change to opponent's turn
     *
     * @param startPosition refers to the (starting) location of piece being moved
     * @param endPosition refers to the final location the piece will end up
     */
    public void move(int startPosition, int endPosition){
        // check legality of move from start to end position
        checkLegalMove(startPosition, endPosition);
        Move move = new Move(this, startPosition, endPosition);
        // process enpassant, promotion and turn data after making move
        if(move.isPawnDoubleMove()){
            // add enpassant possibility
            setEnpassant((startPosition + endPosition) / 2);
        }
        else{   // no enpassant move possible
            setEnpassant(-1);
        }
        move.makeMove();

        // change turn to opposite side
        setTurn(!isWhiteTurn());
    }

    /**
     * Checks that move being made is a legal move
     * @param startPosition refers to the location of piece being moved
     * @param endPosition refers to the final location the piece will end up
     */
    private void checkLegalMove(int startPosition, int endPosition){
        // Check start position has a piece to be moved
        if(!getTile(startPosition).isOccupied()){
            throw new IllegalArgumentException("Piece to be moved cannot be found.");
        }
        if(getTile(endPosition).isOccupied() && getTile(endPosition).getPiece().toString().equals("K")){
            throw new IllegalArgumentException("Illegal to capture enemy King");
        }
        // Check that move being made is legal
        boolean isLegalMove = false;
        for(int legalMoves : getTile(startPosition).getPiece().getLegalMoves()){
            if(legalMoves == endPosition){
                isLegalMove = true;
                break;
            }
        }
        if(!isLegalMove){
            throw new IllegalArgumentException("Illegal move");
        }
    }

    /**
     * Check if the king of the current side is sitting on one of the squares on opponent's attack map
     * if it is, king is being attacked.
     * @return true if king of current side is in check by opponent, else return false
     */
    public boolean isKingChecked(){
        if(isWhiteTurn()){
            return blackAttackMap[getWhiteKingPosition()];
        }
        else{
            return whiteAttackMap[getBlackKingPosition()];
        }
    }

    /**
     * Checks if a tile is being attacked by the opposing team
     * @param tilePosition refers to the index of tile on the chess board
     * @return true if the tile is attacked else return false
     */
    public boolean isTileAttacked(int tilePosition, boolean isWhiteTurn){
        if(isWhiteTurn){
            return blackAttackMap[tilePosition];
        }
        else{
            return whiteAttackMap[tilePosition];
        }
    }

    /**
     * Takes a piece that is attacked and removes it from the board
     * @param position refers to the position of the piece being attacked and removed from the board
     */
    public void removePiece(Integer position){
        if(board[position].getPiece().isWhite()){
            whitePieces.remove(position);
        }
        else{
            blackPieces.remove(position);
        }
    }

    /**
     * Adds a new piece to the board position
     * @param piece refers to the piece type being added
     * @param position refers to the position of the piece
     */
    public void addPiece(Piece piece , int position){
        if(piece.isWhite()){
            whitePieces.add(position);
        }
        else{
            blackPieces.add(position);
        }
    }

    /**
     * Gets the squares that a pawn controls for attack map
     * as pawn has a different logic for defending squares due to specific attacking conditions
     * @param pawnPosition refers to the position of a pawn piece
     * @return a list of squares which the pawn controls based on its attack moves
     */
    private ArrayList<Integer> pawnDefendingSquares(int pawnPosition){
        ArrayList<Integer> list = new ArrayList<>();
        if(board[pawnPosition].getPiece().isWhite()){
            if(getCol(pawnPosition) == 0){  // on the left edge of board, can only control diagonal right square
                list.add(pawnPosition - 7);
            }
            else if(getCol(pawnPosition) == 7){  // on the right edge of board, can only control diagonal left square
                list.add(pawnPosition - 9);
            }
            else{
                list.add(pawnPosition - 7);
                list.add(pawnPosition - 9);
            }
        }
        else{
            if(getCol(pawnPosition) == 0){  // on the left edge of board, can only control diagonal right square
                list.add(pawnPosition + 9);
            }
            else if(getCol(pawnPosition) == 7){  // on the right edge of board, can only control diagonal left square
                list.add(pawnPosition + 7);
            }
            else{
                list.add(pawnPosition + 7);
                list.add(pawnPosition + 9);
            }
        }
        return list;
    }

    public boolean canEnpassant(){
        return getEnpassant() != -1;
    }

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

    public boolean getWhiteKingSideCastle(){
        return whiteKingSideCastle;
    }

    public boolean getWhiteQueenSideCastle(){
        return whiteQueenSideCastle;
    }

    public boolean getBlackKingSideCastle(){
        return blackKingSideCastle;
    }

    public boolean getBlackQueenSideCastle(){
        return blackQueenSideCastle;
    }

    public int getEnpassant(){
        return enpassantPosition;
    }

    /**
     * @return position of pawn that will be captured due to the enpassant move
     */
    public int getEnpassantPawnPosition(){
        if(isWhiteTurn()){
            return getEnpassant() + 8;
        }
        else{
            return getEnpassant() - 8;
        }
    }

    public int getWhiteKingPosition() {
        return whiteKingPosition;
    }

    public int getBlackKingPosition() {
        return blackKingPosition;
    }

    public int getHalfMoveClock(){
        return halfMoveClock;
    }

    public int getFullMoveNum(){
        return fullMoveNum;
    }

    public int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    public int getCol(int position){
        return position % 8;
    }

    /*** SET FUNCTIONS ***/
    public void setTurn(boolean whiteTurn){
        this.isWhiteTurn = whiteTurn;
    }

    public void setKingPosition(int position, boolean isWhiteKing){
        if(isWhiteKing){
            this.whiteKingPosition = position;
        }
        else{
            this.blackKingPosition = position;
        }
    }

    /*** SET CASTLING RIGHTS ***/
    public void setWhiteKingSideCastle(boolean ableToCastle){
        this.whiteKingSideCastle = ableToCastle;
    }

    public void setWhiteQueenSideCastle(boolean ableToCastle){
        this.whiteQueenSideCastle = ableToCastle;
    }

    public void setBlackKingSideCastle(boolean ableToCastle){
        this.blackKingSideCastle = ableToCastle;
    }

    public void setBlackQueenSideCastle(boolean ableToCastle){
        this.blackQueenSideCastle = ableToCastle;
    }

    public void setEnpassant(int position){
        this.enpassantPosition = position;
    }

    public void setWhiteAttackMap(int index, boolean isTrue){
        whiteAttackMap[index] = isTrue;
    }

    public void setBlackAttackMap(int index, boolean isTrue){
        blackAttackMap[index] = isTrue;
    }

    public void setHalfMoveClock(int halfMoveClock){
        this.halfMoveClock = halfMoveClock;
    }

    public void setFullMoveNum(int fullMoveNum){
        this.fullMoveNum = fullMoveNum;
    }

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
        String FEN = "rnbqkbnr/pp1p1ppp/8/2pPp3/8/8/PPP1PPPP/RNBQKBNR w KQkq c6 0 1";
        b.init(FEN);

        b.state();
        System.out.println();
        System.out.println(b.getEnpassant());
//        System.out.print("Moves: ");
//        if (b.isWhiteTurn()) System.out.println("(White)");
//        else System.out.println("(Black)");
//
//        int counter = 0;
//        for(int move : b.getAllLegalMoves()){
//            System.out.print(move + " ");
//            counter++;
//        }
//        System.out.println();
//
//        System.out.println("Count: " + counter);
//        System.out.print("King position: ");
//        if (b.isWhiteTurn()) System.out.println(b.getWhiteKingPosition());
//        else System.out.println(b.getBlackKingPosition());
//        System.out.println("FEN: " + FEN);

        while(true){
            if (b.isWhiteTurn()) System.out.println("(White)");
            else System.out.println("(Black)");
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter start position of piece to move: ");
            int start;
            start = sc.nextInt();
            while(start < 0 || start > 63 || !b.getTile(start).isOccupied()
                    || b.getTile(start).getPiece().isWhite() != b.isWhiteTurn()
                    || b.getTile(start).getPiece().getLegalMoves().size() == 0){
                System.out.println("Enter start position of piece to move: ");
                start = sc.nextInt();
            }
            System.out.println("Legal Moves: ");
            for(int move : b.getTile(start).getPiece().getLegalMoves()){
                System.out.print(move + " ");
            }
            System.out.println();
            System.out.println("Enter end position of piece: ");
            int end = sc.nextInt();
            b.move(start, end);
            b.state();
            System.out.println(b.getEnpassant());
        }

    }
}
