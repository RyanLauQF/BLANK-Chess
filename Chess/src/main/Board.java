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
    private final PieceList whitePieces;
    private final PieceList blackPieces;

    // used for fast checking if king is in check
    private int whiteKingPosition;
    private int blackKingPosition;

    // a map to show the location of pinned pieces
    private int[] pinnedList;
    private Stack<Integer> resetPinnedList;

    /**
     * Board constructor
     */
    public Board(){
        this.board = new Tile[64];
        // initiate array list to keep track of position of all pieces on the board for each side
        this.whitePieces = new PieceList();
        this.blackPieces = new PieceList();
        this.pinnedList = new int[64];
    }

    /**
     *  Takes in a FEN (Forsyth–Edwards Notation) and converts the data onto the chess board
     *  Initiates attack data for both side pieces
     * @param FEN refers to a standard chess notation of a particular position
     */
    public void init(String FEN){
        // set board to default position / custom FEN position
        FENUtilities.convertFENtoBoard(FEN, this);
    }

    /**
     * Gets all legal moves on the board for the current side's turn
     * E.g. if its white's turn, get all white legal moves.
     * @return list of positions of all legal moves on the board
     */
    public ArrayList<Short> getAllLegalMoves(){
        ArrayList <Short> moveList = new ArrayList<>();
        // pseudo-legal moves are filtered out when generating moves from individual pieces
        if (isWhiteTurn()){  // get all white legal moves
            PieceList list = getWhitePieces();
            for(int i = 0; i < list.getCount(); i++){
                moveList.addAll(board[list.occupiedTiles[i]].getPiece().getLegalMoves()); // merge list
            }
        }
        else{   // get all black legal moves
            PieceList list = getBlackPieces();
            for(int i = 0; i < list.getCount(); i++){ // goes through all white pieces
                moveList.addAll(board[list.occupiedTiles[i]].getPiece().getLegalMoves()); // merge list
            }
        }
        return moveList;
    }

    /**
     * Check if the king of the current side is being attacked by searching all possible squares
     * @return true if king of current side is in check by opponent, else return false
     */
    public int kingCheckedCount(boolean isWhiteKing){
        int kingPosition;
        int checkCount = 0;

        if(isWhiteKing){
            kingPosition = getWhiteKingPosition();
        }
        else{
            kingPosition = getBlackKingPosition();
        }

        // at the king position, search in all directions.
        int end, offSet;
        boolean alliedPieceFound = false;
        int alliedPieceLocation = 0;
        int[] directions = MoveDirections.getDirections(kingPosition);

        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = kingPosition + (offSet * (i + 1));
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    // if it is an enemy piece
                    if(piece.isWhite() != isWhiteKing){
                        if(i == 0){
                            // check for enemy king 1 tile away from current king
                            if(piece.isKing()){
                                checkCount++;
                                return checkCount;  // do not bother to continue finding for double check
                            }
                        }
                        if(piece.isPawn() || piece.isKnight() || piece.isKing()){
                            break;
                        }
                        if(index < 4){  // for straight directions, check if it is a rook / queen
                            if(piece.isRook() || piece.isQueen()){
                                if(alliedPieceFound){
                                    setPinned(end, offSet);
                                }
                                else{
                                    checkCount++;
                                }
                                // break out of current direction loop to go to next offset direction
                                break;
                            }
                        }
                        else{   // diagonal directions
                            if(piece.isBishop() || piece.isQueen()){
                                if(alliedPieceFound){
                                    setPinned(alliedPieceLocation, offSet);
                                }
                                else{
                                    checkCount++;
                                }
                                // break out of current direction loop to go to next offset direction
                                break;
                            }
                        }
                    }
                    // if it is an allied piece, check for pinned in the direction
                    else{
                        // if there was already an allied piece found in the same direction beforehand,
                        // break out of loop as there will confirm be no pinned piece
                        if(alliedPieceFound){
                            break;
                        }
                        alliedPieceFound = true;
                        alliedPieceLocation = end;
                    }
                }
            }
            alliedPieceFound = false;
            // can stop looking further as it is a double check and only king moves are allowed
            if(checkCount > 1) return checkCount;
        }

        // search for enemy pawn attacking
        checkCount = checkPawnAttacking(isWhiteKing, kingPosition, checkCount);
        if(checkCount > 1) return checkCount;

        int[] knightSquares = MoveDirections.knightOffSets;
        // search knight attacking squares
        for(int i = 0; i < 8; i++){
            end = kingPosition + knightSquares[i];
            // if it is a valid knight move
            if(Math.abs(getRow(kingPosition) - getRow(end)) + Math.abs(getCol(kingPosition) - getCol(end)) == 3
                && end > 0 && end < 64){
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    if(piece.isKnight() && (piece.isWhite() != isWhiteKing)){
                        checkCount++;
                        if(checkCount > 1) return checkCount;
                    }
                }
            }
        }
        return checkCount;
    }

    private int checkPawnAttacking(boolean isWhiteKing, int kingPosition, int checkCounter){
        int rightPawnIndex;
        int leftPawnIndex;

        if(isWhiteKing){
            // set black pawn locations relative to white king
            leftPawnIndex = kingPosition -9;
            rightPawnIndex = kingPosition -7;
        }
        else{
            // set white pawn location relative to black king
            leftPawnIndex = kingPosition + 7;
            rightPawnIndex = kingPosition + 9;
        }

        boolean rightEdgeKing = false;
        boolean leftEdgeKing = false;

        if(kingPosition % 8 == 0){ // king on left edge
            leftEdgeKing = true;
        }
        else if (kingPosition % 8 == 7){  // king on right edge
            rightEdgeKing = true;
        }

        if(!rightEdgeKing && checkBound(rightPawnIndex) && getTile(rightPawnIndex).isOccupied()){
            Piece piece = getTile(rightPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhiteKing){
                checkCounter++;
            }
        }
        if(!leftEdgeKing && checkBound(leftPawnIndex) && getTile(leftPawnIndex).isOccupied()){
            Piece piece = getTile(leftPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhiteKing){
                checkCounter++;
            }
        }
        return checkCounter;
    }

    private boolean checkBound(int index){
        return index >= 0 && index <= 63;
    }

    public void setPinned(int position, int pinType){
        pinnedList[position] = pinType;
    }

    public boolean isPinned(int position){
        return pinnedList[position] != 0;
    }

    public int getPinType(int position){
        return pinnedList[position];
    }

    public void resetPinnedList(){
        while(!resetPinnedList.isEmpty()){
            int resetPosition = resetPinnedList.pop();
            pinnedList[resetPosition] = 0;
        }
    }

    /**
     * Checks if a tile is being attacked by the opposing team by searching outwards
     * @param tilePosition refers to the index of tile on the chess board
     * @return true if the tile is attacked else return false
     */
    public boolean isTileAttacked(int tilePosition, boolean isWhiteTurn){
        int leftPawnPosition;
        int rightPawnPosition;

        if(isWhiteTurn){
            // set black pawn locations relative to white king
            leftPawnPosition = - 9;
            rightPawnPosition = -7;
        }
        else{
            // set white pawn location relative to black king
            leftPawnPosition = 7;
            rightPawnPosition = 9;
        }

        // at the tile position, search in all directions.
        int[] directions = MoveDirections.getDirections(tilePosition);
        int[] knightSquares = MoveDirections.knightOffSets;
        int attackCount = 0;
        int end, offSet;

        // search enemy pawn attacking squares to check if enemy pawn is checking king
        if(getTile(tilePosition + leftPawnPosition).isOccupied()){
            Piece piece = getTile(tilePosition + leftPawnPosition).getPiece();
            if(piece.isPawn() && (piece.isWhite() != isWhiteTurn)){
                attackCount++;
            }
        }
        if(getTile(tilePosition + rightPawnPosition).isOccupied()){
            Piece piece = getTile(tilePosition + rightPawnPosition).getPiece();
            if(piece.isPawn() && (piece.isWhite() != isWhiteTurn)){
                attackCount++;
            }
        }

        // Search in directions of all offsets first
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = tilePosition + (offSet * (i + 1));
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    // if it is an enemy piece
                    if(piece.isWhite() != isWhiteTurn){
                        if(index < 4){  // for straight directions, check if it is a rook / queen
                            if(piece.isRook() || piece.isQueen()){
                                attackCount++;
                                // break out of current direction loop to go to next offset direction
                                break;
                            }
                        }
                        else{   // diagonal directions
                            if(piece.isBishop() || piece.isQueen()){
                                attackCount++;
                                // break out of current direction loop to go to next offset direction
                                break;
                            }
                        }
                    }
                    // if it is an allied piece blocking, do not need to continue search in the direction
                    break;
                }
            }
            // can stop looking further as it is a double check and only king moves are allowed
            if(attackCount > 0) return true;
        }

        // search knight attacking squares
        for(int i = 0; i < 8; i++){
            end = tilePosition + knightSquares[i];
            // if it is a valid knight move
            if(end < 0 || end > 63){
                continue;
            }
            if(Math.abs(getRow(tilePosition) - getRow(end)) + Math.abs(getCol(tilePosition) - getCol(end)) == 3
                && getTile(end).isOccupied()){
                Piece piece = getTile(end).getPiece();
                if(piece.isKnight() && (piece.isWhite() != isWhiteTurn)){
                    attackCount++;
                    if(attackCount > 0) return true;
                }
            }
        }
        return false;
    }

    /**
     * If a pawn has reached the end of the board, promote the pawn to either a Rook, Bishop, Knight or Queen
     * @param pieceType refers to the choice of piece to promote the pawn to
     * @param pieceTile refers to the tile on the board containing the pawn that has to be updated
     */
    public void promote(Piece.PieceType pieceType, Tile pieceTile){
        Piece piece = pieceTile.getPiece();
        if(pieceType == Piece.PieceType.QUEEN){
            // Promote the pawn to a queen
            pieceTile.setPiece(new Queen(piece.isWhite(), piece.getPosition(), this));
        }
    }

    /**
     * Takes a piece that is attacked and removes it from the board
     * @param position refers to the position of the piece being attacked and removed from the board
     */
    public void removePiece(Integer position){
        if(board[position].getPiece().isWhite()){
            whitePieces.removePiece(position);
        }
        else{
            blackPieces.removePiece(position);
        }
    }

    /**
     * Adds a new piece to the board position
     * @param piece refers to the piece type being added
     * @param position refers to the position of the piece
     */
    public void addPiece(Piece piece , int position){
        if(piece.isWhite()){
            whitePieces.addPiece(position);
        }
        else{
            blackPieces.addPiece(position);
        }
    }

    /**
     * Checks if the current side has any castling rights
     * @return true if either king side or queen side castling is present for the current side
     */
    public boolean hasCastlingRights(){
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
    public void setRookSideCastling(boolean isWhiteRook, int rookPosition, boolean setCastling){
        if(isWhiteRook){
            if(rookPosition == 63){
                setWhiteKingSideCastle(setCastling);
            }
            else if(rookPosition == 56){
                setWhiteQueenSideCastle(setCastling);
            }
        }
        else{
            if(rookPosition == 7){
                setBlackKingSideCastle(setCastling);
            }
            else if(rookPosition == 0){
                setBlackQueenSideCastle(setCastling);
            }
        }
    }

//  /******** GETTER FUNCTIONS ********/
//  ------------------------------------
    public PieceList getWhitePieces(){
        return whitePieces;
    }

    public PieceList getBlackPieces(){
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
        String FEN = "r3kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R b KQkq - 0 1";
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
        int start, end;
        while(b.getAllLegalMoves().size() != 0){
            if (b.isWhiteTurn()) System.out.println("(White)");
            else System.out.println("(Black)");
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter start position of piece to move: ");
            start = sc.nextInt();
            while(start < 0 || start > 63 || !b.getTile(start).isOccupied()
                    || b.getTile(start).getPiece().isWhite() != b.isWhiteTurn()
                    || b.getTile(start).getPiece().getLegalMoves().size() == 0){
                System.out.println("Enter start position of piece to move: ");
                start = sc.nextInt();
            }
            System.out.println("Legal Moves: ");
            for(short move : b.getTile(start).getPiece().getLegalMoves()){
                System.out.print(MoveGenerator.getEnd(move) + " ");
            }
            System.out.println();
            System.out.println("Enter end position of piece: ");
            end = sc.nextInt();
//            while(end < 0 || end > 63 || !b.getTile(start).getPiece().isLegalMove(end)){
//                System.out.println("Enter end position of piece: ");
//                end = sc.nextInt();
//            }
            Move move = new Move(b, start, end);
            move.makeMove();
            b.state();
            System.out.println("Enpassant: " + b.getEnpassant());
        }
        // Check how game has ended
        GameStatus.checkGameEnded(b);
    }
}
