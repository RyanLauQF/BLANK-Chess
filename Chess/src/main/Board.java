import java.util.*;

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

    // used for evaluation to check king safety
    private boolean hasWhiteKingCastled;
    private boolean hasBlackKingCastled;

    // a map to show the location of pinned pieces
    private final int[] pinnedList;
    private final Stack<Integer> resetPinnedList;

    // keep track of the number of checks the king for either side is under
    private int checkCount;
    private int attackingPieceLocation;
    private int attackingOffSet;

    // keeps track of the previous move made on the board. If at start state, initialise to 0
    private Move previousMove;

    /**
     * Board constructor
     */
    public Board(){
        this.board = new Tile[64];
        // initiate array list to keep track of position of all pieces on the board for each side
        this.whitePieces = new PieceList();
        this.blackPieces = new PieceList();
        this.pinnedList = new int[64];
        this.resetPinnedList = new Stack<>();
        this.checkCount = 0;
        this.previousMove = null;
        this.hasWhiteKingCastled = false;
        this.hasBlackKingCastled = false;
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
        // calculate the number of enemies attacking the king
        checkCount = kingCheckedCount(isWhiteTurn());
        // if it is in double check, only king can move
        if(checkCount >= 2){
            int kingPosition = getKingPosition(isWhiteTurn());
            moveList.addAll(board[kingPosition].getPiece().getLegalMoves());
        }
        else {
            PieceList list = getPieceList(isWhiteTurn());
            // pseudo-legal moves are filtered out when generating moves from individual pieces
            for (int i = 0; i < list.getCount(); i++) {
                moveList.addAll(board[list.occupiedTiles[i]].getPiece().getLegalMoves()); // merge list
            }
        }
        resetPinnedList();
        return moveList;
    }

    public ArrayList<Short> getAllCaptures(){
        ArrayList <Short> moveList = new ArrayList<>();
        // calculate the number of enemies attacking the king
        checkCount = kingCheckedCount(isWhiteTurn());
        // if it is in double check, only king can move
        if(checkCount >= 2){
            int kingPosition = getKingPosition(isWhiteTurn());
            moveList.addAll(board[kingPosition].getPiece().getCaptureMoves());
        }
        else {
            PieceList list = getPieceList(isWhiteTurn());
            // pseudo-legal moves are filtered out when generating moves from individual pieces
            for (int i = 0; i < list.getCount(); i++) {
                moveList.addAll(board[list.occupiedTiles[i]].getPiece().getCaptureMoves()); // merge list
            }
        }
        resetPinnedList();
        return moveList;
    }

    /**
     * Check if the king of the current side is being attacked by searching all possible squares
     * @return true if king of current side is in check by opponent, else return false
     */
    public int kingCheckedCount(boolean isWhiteKing){
        int kingPosition = getKingPosition(isWhiteKing);
        int checkCount = 0;
        attackingPieceLocation = -1;
        attackingOffSet = 0;

        // at the king position, search in all directions.
        int end, offSet;
        boolean alliedPieceFound = false;
        int alliedPieceLocation = 0;

        int[] knightSquares = MoveDirections.knightOffSets;
        // search knight attacking squares
        for(int i = 0; i < 8; i++){
            end = kingPosition + knightSquares[i];
            // if it is a valid knight move
            if(Math.abs(getRow(kingPosition) - getRow(end)) + Math.abs(getCol(kingPosition) - getCol(end)) == 3
                    && end >= 0 && end < 64){
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    if(piece.isKnight() && (piece.isWhite() != isWhiteKing)){
                        if(checkCount == 0) attackingPieceLocation = end;
                        checkCount++;
                        if(checkCount > 1) return checkCount;
                    }
                }
            }
        }

        int[] directions = MoveDirections.getDirections(kingPosition);
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = kingPosition + (offSet * (i + 1));
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    // if it is an enemy piece
                    if(piece.isWhite() != isWhiteKing){
                        if(piece.isPawn() || piece.isKnight() || piece.isKing()){
                            break;
                        }
                        if(index < 4){  // for straight directions, check if it is a rook / queen
                            if(piece.isRook() || piece.isQueen()){
                                if(alliedPieceFound){
                                    setPinned(alliedPieceLocation, offSet);
                                }
                                else{
                                    if(checkCount == 0){
                                        attackingPieceLocation = end;
                                        attackingOffSet = offSet;
                                    }
                                    checkCount++;
                                }
                            }
                        }
                        else{   // diagonal directions
                            if(piece.isBishop() || piece.isQueen()){
                                if(alliedPieceFound){
                                    setPinned(alliedPieceLocation, offSet);
                                }
                                else{
                                    if(checkCount == 0) {
                                        attackingPieceLocation = end;
                                        attackingOffSet = offSet;
                                    }
                                    checkCount++;
                                }
                            }

                        }
                        // break out of current direction loop to go to next offset direction
                        break;
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
            // can stop looking further as it is a double check and only king moves are allowed
            if(checkCount > 1) return checkCount;
            alliedPieceFound = false;
        }

        // search for enemy pawn attacking
        checkCount = checkPawnAttacking(isWhiteKing, kingPosition, checkCount);
        return checkCount;
    }

    /**
     * Checks if the current piece is being attacked by any opposing pawns
     * @param isWhitePiece refers to the side of the piece being attacked
     * @param piecePosition refers to the position of the piece
     * @param checkCounter refers to the current number of attacks on the piece (if any)
     * @return the number of pawns attacking the piece + checkCounter (if checkCounter is being used)
     */
    public int checkPawnAttacking(boolean isWhitePiece, int piecePosition, int checkCounter){
        int rightPawnIndex;
        int leftPawnIndex;

        if(isWhitePiece){
            // set black pawn locations relative to white piece
            leftPawnIndex = piecePosition -9;
            rightPawnIndex = piecePosition -7;
        }
        else{
            // set white pawn location relative to black piece
            leftPawnIndex = piecePosition + 7;
            rightPawnIndex = piecePosition + 9;
        }

        boolean rightEdgePiece = false;
        boolean leftEdgePiece = false;

        if(piecePosition % 8 == 0){ // piece on left edge
            leftEdgePiece = true;
        }
        else if (piecePosition % 8 == 7){  // piece on right edge
            rightEdgePiece = true;
        }

        if(!rightEdgePiece && checkBound(rightPawnIndex) && getTile(rightPawnIndex).isOccupied()){
            Piece piece = getTile(rightPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhitePiece){
                if(checkCount == 0) attackingPieceLocation = rightPawnIndex;
                checkCounter++;
            }
        }
        if(!leftEdgePiece && checkBound(leftPawnIndex) && getTile(leftPawnIndex).isOccupied()){
            Piece piece = getTile(leftPawnIndex).getPiece();
            if(piece.isPawn() && piece.isWhite() != isWhitePiece){
                if(checkCount == 0) attackingPieceLocation = leftPawnIndex;
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
        resetPinnedList.add(position);
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
        checkCount = 0;
    }

    public boolean isKingChecked(){
        return isTileAttacked(getKingPosition(isWhiteTurn()), isWhiteTurn());
    }

    /**
     * Checks if a tile is being attacked by the opposing team by searching outwards
     * @param tilePosition refers to the index of tile on the chess board
     * @param isWhiteTurn refers to the side being attacked
     * @return true if the tile is attacked else return false
     */
    public boolean isTileAttacked(int tilePosition, boolean isWhiteTurn){
        // at the tile position, search in all directions.
        int end, offSet;
        int[] knightSquares = MoveDirections.knightOffSets;
        // search knight attacking squares
        for(int i = 0; i < 8; i++){
            end = tilePosition + knightSquares[i];
            // if it is a valid knight move
            if(Math.abs(getRow(tilePosition) - getRow(end)) + Math.abs(getCol(tilePosition) - getCol(end)) == 3
                    && end >= 0 && end < 64){
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    if(piece.isKnight() && (piece.isWhite() != isWhiteTurn)){
                        return true;
                    }
                }
            }
        }

        int[] directions = MoveDirections.getDirections(tilePosition);
        // Search in directions of all offsets first
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = tilePosition + (offSet * (i + 1));
                if(getTile(end).isOccupied()){
                    Piece piece = getTile(end).getPiece();
                    // if it is an enemy piece
                    if(piece.isWhite() != isWhiteTurn){
                        if(i == 0){
                            // check for enemy king 1 tile away from current king
                            if(piece.isKing()){
                                return true;
                            }
                        }
                        if(piece.isPawn() || piece.isKnight() || piece.isKing()){
                            break;
                        }
                        if(index < 4){  // for straight directions, check if it is a rook / queen
                            if(piece.isRook() || piece.isQueen()){
                                return true;
                            }
                        }
                        else{   // diagonal directions
                            if(piece.isBishop() || piece.isQueen()){
                                return true;
                            }
                        }
                    }
                    // since piece has been found, break from direction
                    break;
                }
            }
        }
        // search for enemy pawn attacking
        return checkPawnAttacking(isWhiteTurn, tilePosition, 0) != 0;
    }

    public HashSet<Integer> getCounterCheckSquares(){
        // only if being checked by a rook / bishop / queen there is a need to generate counter check squares
        // keeps track of the squares that pieces can move to when king is under check (blocking / capture attacking piece)
        HashSet<Integer> counterCheckSquares = new HashSet<>();
        counterCheckSquares.add(getAttackingPieceLocation());
        // take the opposite of the attacking offset to get offset towards the checked king
        int endPosition = getAttackingPieceLocation() - attackingOffSet;
        // get all the squares which are under attack and add them to the hashSet
        while(!getTile(endPosition).isOccupied()){
            counterCheckSquares.add(endPosition);
            endPosition -= attackingOffSet;
        }
        return counterCheckSquares;
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
        else if(pieceType == Piece.PieceType.KNIGHT){
            pieceTile.setPiece(new Knight(piece.isWhite(), piece.getPosition(), this));
        }
        else if(pieceType == Piece.PieceType.ROOK){
            pieceTile.setPiece(new Rook(piece.isWhite(), piece.getPosition(), this));
        }
        else if(pieceType == Piece.PieceType.BISHOP){
            pieceTile.setPiece(new Bishop(piece.isWhite(), piece.getPosition(), this));
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

    public void setHasKingCastled(boolean hasCastled, boolean isWhiteKing){
        if(isWhiteKing){
            hasWhiteKingCastled = hasCastled;
        }
        else{
            hasBlackKingCastled = hasCastled;
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

    public boolean hasKingSideCastling(boolean isWhite){
        if(isWhite){
            return getWhiteKingSideCastle();
        }
        else{
            return getBlackKingSideCastle();
        }
    }

    public boolean hasQueenSideCastling(boolean isWhite){
        if(isWhite){
            return getWhiteQueenSideCastle();
        }
        else{
            return getBlackQueenSideCastle();
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

    public PieceList getPieceList(boolean isWhite){
        if(isWhite){
            return whitePieces;
        }
        else{
            return blackPieces;
        }
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

    public boolean kingHasCastled(boolean isWhiteKing){
        if(isWhiteKing){
            return hasWhiteKingCastled;
        }
        else{
            return hasBlackKingCastled;
        }
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

    public int getKingPosition(boolean isWhiteKing){
        if(isWhiteKing){
            return getWhiteKingPosition();
        }
        else{
            return getBlackKingPosition();
        }
    }

    public Piece getAttackingPiece(){
        return getTile(attackingPieceLocation).getPiece();
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

    public int getCheckCount(){
        return checkCount;
    }

    public Move getPreviousMove(){
        return previousMove;
    }

    public int getAttackingPieceLocation(){
        return attackingPieceLocation;
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

    public void setPreviousMove(Move movement){
        this.previousMove = movement;
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
    public static void main(String[] args) {
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
            while(end < 0 || end > 63){
                System.out.println("Enter end position of piece: ");
                end = sc.nextInt();
            }
            Move move = new Move(b, MoveGenerator.generateMove(start, end, 0));
            move.makeMove();
            b.state();
            System.out.println("Enpassant: " + b.getEnpassant());
        }
        // Check how game has ended
        GameStatus.checkGameEnded(b);
    }
}
