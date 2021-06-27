import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

/*
 *     Board class used in Chess-Engine to keep track of the board state for each move
 *     Copyright (C) 2021 Ryan Lau Q. F.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * @author Ryan Lau Q. F.
 */
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

    /**
     * Calculates data to get attack map of white pieces (all squares that white pieces control)
     */
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

    /**
     * Calculates data to get attack map of black pieces (all squares that black pieces control)
     */
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
            Integer[] whitePieces = new Integer[getWhitePieces().size()];
            getWhitePieces().toArray(whitePieces);
            for(int each : whitePieces){ // goes through all white pieces
                moveList.addAll(board[each].getPiece().getLegalMoves()); // merge list
            }
        }
        else{   // get all black legal moves
            Integer[] blackPieces = new Integer[getBlackPieces().size()];
            getBlackPieces().toArray(blackPieces);
            for(int each : blackPieces){ // goes through all white pieces
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
     *      2) Castling rights
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

        // disables castling rights if either king or rook is moving
        if(hasCastlingRights()){
            // disable both king and queen side castling if king is moving
            if(getTile(startPosition).getPiece().toString().equals("K")){
                if(isWhiteTurn()){
                    setWhiteKingSideCastle(false);
                    setWhiteQueenSideCastle(false);
                }
                else{
                    setBlackKingSideCastle(false);
                    setBlackQueenSideCastle(false);
                }
            }
            // disable castling rights if a rook is moving
            else if(getTile(startPosition).getPiece().toString().equals("R")){
                disableRookSideCastling(isWhiteTurn(), startPosition);
            }
        }
        // makes the move
        Move move = new Move(this, startPosition, endPosition);
        // process enpassant
        int enpassantPosition = -1;
        if(move.isPawnDoubleMove()){
            // add enpassant possibility
            enpassantPosition = (startPosition + endPosition) / 2;
        }
        move.makeMove();
        setEnpassant(enpassantPosition);
        // process promotion and turn data after making move
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

    /**
     * Checks if the current side has any castling rights
     * @return true if either king side or queen side castling is present for the current side
     */
    private boolean hasCastlingRights(){
        if(isWhiteTurn()){
            return getWhiteKingSideCastle() || getWhiteQueenSideCastle();
        }
        else{
            return getBlackKingSideCastle() || getBlackQueenSideCastle();
        }
    }

    /**
     * If a rook of either side has moved from its starting position, disable castling for that side
     * @param isWhiteRook refers to the side which the rook is on
     * @param rookPosition refers to the position of the rook
     */
    private void disableRookSideCastling(boolean isWhiteRook, int rookPosition){
        if(isWhiteRook){
            if(rookPosition == 63){
                setWhiteKingSideCastle(false);
            }
            else if(rookPosition == 56){
                setWhiteQueenSideCastle(false);
            }
        }
        else{
            if(rookPosition == 7){
                setBlackKingSideCastle(false);
            }
            else if(rookPosition == 0){
                setBlackQueenSideCastle(false);
            }
        }
    }

//  /******** GETTER FUNCTIONS ********/
//  ------------------------------------
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

    public boolean canEnpassant(){
        return getEnpassant() != -1;
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

    /**
     * Gets the row which the current index belongs to on the chess board
     * @param position refers to the index on the board
     * @return the row of the index (i.e. index 8 is on row 1)
     */
    public int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    /**
     * Gets the column which the current index belongs to on the chess board
     * @param position refers to the index on the board
     * @return the column of the index (i.e. index 8 is on column 0)
     */
    public int getCol(int position){
        return position % 8;
    }

//  /******** SET FUNCTIONS ********/
//  ---------------------------------

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

    /**
     * Prints out state of board to show location of pieces for each side
     * along with the index of tiles on the board to compare
     */
    public void state(){
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

    /**
     * Unit testing
     * > Able to play Chess on CLI. (Promotion has not been implemented)
     */
    public static void main(String[] args){
        Board b = new Board();
        // Custom FEN input
        String FEN = "r3kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b.init(FEN);

        b.state();
        System.out.println();
        System.out.println("Enpassant: " + b.getEnpassant());

//        ** USED FOR DEBUGGING INDIVIDUAL BOARD MOVES **
//        -----------------------------------------------
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
//        -----------------------------------------------

        // Used to play CLI chess
        while(b.getAllLegalMoves().size() != 0){
            if (b.isWhiteTurn()) System.out.println("(White)");
            else System.out.println("(Black)");
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter start position of piece to move: ");
            int start = sc.nextInt();
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
            while(end < 0 || end > 63 || !b.getTile(start).getPiece().isLegalMove(end)){
                System.out.println("Enter end position of piece: ");
                end = sc.nextInt();
            }
            b.move(start, end);
            b.state();
            System.out.println("Enpassant: " + b.getEnpassant());
        }
        // Check how game has ended
        GameStatus.checkGameEnded(b);
    }
}
