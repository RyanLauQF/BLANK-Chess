import java.util.ArrayList;
import java.util.HashSet;

public abstract class Piece {

    public enum PieceType {
        PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }

    protected final boolean colour; // White == 1, Black == 0;
    protected int position; // position of piece on board
    protected Board board; // reference to the chess board
    protected PieceType type;

    /**
     * Piece Constructor
     * @param isWhite refers to the side which the piece is on (White / Black)
     * @param position refers to the position of the piece
     * @param board refers to a reference to the chess board
     */
    public Piece(boolean isWhite, int position, Board board){
        if(position < 0 || position > 63 || board == null){
            throw new IllegalArgumentException();
        }
        this.position = position;
        this.colour = isWhite;
        this.board = board;
    }

    /**
     * Method returns all legals moves of a piece by filtering out all illegal moves
     * and converting pseudo-legal moves to legal moves via checking if king is placed in check
     * @return A list of moves (end position) available for the piece
     */
    public ArrayList<Short> getLegalMoves(){
        ArrayList<Short> moveList = new ArrayList<>();
        ArrayList<Short> moveSquares;

        if(board.getCheckCount() == 1 && !this.isKing()){
             /*
                Possible moves if king is in single check:
                    1) Move the king out of check (check all king valid moves)
                    2) Capture checking piece
                    3) Block the checking piece (if the attacking piece is a sliding piece)
            */
            if(this.isPinned()){    // if piece is pinned, only generate the moves along pin axis
                moveSquares = generatePinnedMoves(new ArrayList<>(), false);
            }
            else{
                moveSquares = getPossibleMoves(false);
            }
            int end;
            if(board.getAttackingPiece().isSliderPiece()){
                HashSet<Integer> possibleMoves = board.getCounterCheckSquares();
                for (Short moves : moveSquares) {
                    end = MoveGenerator.getEnd(moves);
                    if (possibleMoves.contains(end)) {
                        if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                            generatePawnPromotionMoves(moves, moveList);
                        }
                        else{
                            moveList.add(moves);
                        }
                    }
                }
            }
            else{   // only need to capture the piece
                for (Short moves : moveSquares) {
                    end = MoveGenerator.getEnd(moves);
                    if(this.isPawn()){  // if this piece is a pawn, check if can capture enpassant
                        if (end == board.getAttackingPieceLocation() || end == board.getEnpassant()) {
                            if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                                generatePawnPromotionMoves(moves, moveList);
                            }
                            else{
                                moveList.add(moves);
                            }
                        }
                    }
                    else{
                        if (end == board.getAttackingPieceLocation()) {
                            moveList.add(moves);
                        }
                    }
                }
            }
            return moveList;
        }
        else if(this.isPinned()){
            return generatePinnedMoves(moveList, false);
        }
        else{
            moveSquares = getPossibleMoves(false);
            int kingPosition;
            for(short moves : moveSquares){
                // The moves are currently pseudo-legal, test if king is in check to get legal moves
                Move movement = new Move(board, moves);
                movement.makeMove();    // make the move on the board without making a copy
                kingPosition = board.getKingPosition(this.isWhite());
                // if king is not under check after making the move, the move is legal.
                if(!board.isTileAttacked(kingPosition, this.isWhite())) {
                    movement.unMake();
                    if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                        generatePawnPromotionMoves(moves, moveList);
                    }
                    else{
                        moveList.add(moves);
                    }
                }
                else{
                    movement.unMake();  // revert board back to its original state
                }
            }
        }
        return moveList;
    }

    public ArrayList<Short> generatePinnedMoves(ArrayList<Short> moveList, boolean generateCapturesOnly){
        // if a knight is pinned it cannot move
        if(this.isKnight()){
            return moveList;
        }
        int pinOffSet = board.getPinType(getPosition());
        int absMath = Math.abs(pinOffSet);
        if(absMath == 7 || absMath == 9){
            // diagonal pin
            if(isRook()){   // rook cannot move along diagonal pin
                return moveList;
            }
            // pawn can only attack along offset
            else if(isPawn()){
                if((isWhite() && pinOffSet > 0) || (!isWhite() && pinOffSet < 0)) {
                    return moveList;
                }
                ArrayList<Short> legalMoves = new ArrayList<>();
                Pawn.generatePawnAttackMoves(Pawn.getPawnDirections(this.isWhite(), getPosition()), this, moveList);
                int diff, end;
                for(Short moves: moveList){
                    end = MoveGenerator.getEnd(moves);
                    diff = Math.abs(getPosition() - end);
                    if(diff == absMath){
                        if(Pawn.canPromote(this.isWhite(), end)){
                            generatePawnPromotionMoves(moves, legalMoves);
                        }
                        else{
                            legalMoves.add(moves);
                        }
                    }
                }
                return legalMoves;
            }
        }
        else if(absMath == 1 || absMath == 8){
            // horizontal / vertical pin
            if(isBishop()){ // bishop cannot move along a straight pin
                return moveList;
            }
            // pawn can only push
            else if(isPawn()){
                if(generateCapturesOnly){
                    return moveList;    // do not generate pawn push moves
                }
                if(absMath == 1) return moveList;
                // generate pawn push moves
                ArrayList<Short> legalMoves = new ArrayList<>();
                Pawn.generatePawnPushMoves(Pawn.getPawnDirections(this.isWhite(), getPosition())[0], this, moveList);
                int diff, end;
                for(Short moves: moveList){
                    end = MoveGenerator.getEnd(moves);
                    diff = Math.abs(getPosition() - end);
                    if(diff % 8 == 0){
                        if(Pawn.canPromote(this.isWhite(), end)){
                            generatePawnPromotionMoves(moves, legalMoves);
                        }
                        else{
                            legalMoves.add(moves);
                        }
                    }
                }
                return legalMoves;
            }
        }

        // generate moves towards the enemy piece by following pinned offset
        int endPosition = getPosition() + pinOffSet;
        while(!board.getTile(endPosition).isOccupied()){
            if(!generateCapturesOnly){
                moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
            }
            endPosition += pinOffSet;
        }
        moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 4));    // capture the enemy piece

        // reverse pinned offset to get offset towards king and generate moves towards the king
        if(!generateCapturesOnly){
            endPosition = getPosition() - pinOffSet;
            while(!board.getTile(endPosition).isOccupied()){
                moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
                endPosition -= pinOffSet;
            }
        }
        return moveList;
    }

    public final void generatePawnPromotionMoves(short move, ArrayList<Short> moveList){
        // if the piece is a pawn and can be promoted, add promotion moves instead of normal move
        int startPosition = MoveGenerator.getStart(move);
        int endPosition = MoveGenerator.getEnd(move);
        if(MoveGenerator.isCapture(move)){     // generate capture-promotion moves
            for(int promotionIndex = 12; promotionIndex < 16; promotionIndex++){
                moveList.add(MoveGenerator.generateMove(startPosition, endPosition, promotionIndex));
            }
        }
        else{   // generate standard promotion moves
            for(int promotionIndex = 8; promotionIndex < 12; promotionIndex++){
                moveList.add(MoveGenerator.generateMove(startPosition, endPosition, promotionIndex));
            }
        }
    }

    public ArrayList<Short> getCaptureMoves(){
        ArrayList<Short> moveList = new ArrayList<>();
        ArrayList<Short> moveSquares;

        if(board.getCheckCount() == 1 && !this.isKing()){
             /*
                Possible moves if king is in single check:
                    1) Move the king out of check (check all king valid moves)
                    2) Capture checking piece
                    3) Block the checking piece (if the attacking piece is a sliding piece)
            */
            if(this.isPinned()){    // if piece is pinned, only generate the moves along pin axis
                moveSquares = generatePinnedMoves(new ArrayList<>(), true);
            }
            else{
                moveSquares = getPossibleMoves(true);
            }
            int end;
            for (Short moves : moveSquares) {
                end = MoveGenerator.getEnd(moves);
                if(this.isPawn()){  // if this piece is a pawn, check if can capture enpassant
                    if (end == board.getAttackingPieceLocation() || end == board.getEnpassant()) {
                        if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                            generatePawnPromotionMoves(moves, moveList);
                        }
                        else{
                            moveList.add(moves);
                        }
                    }
                }
                else{
                    if (end == board.getAttackingPieceLocation()) {
                        moveList.add(moves);
                    }
                }
            }
            return moveList;
        }
        else if(this.isPinned()){
            return generatePinnedMoves(moveList, true);
        }
        else{
            moveSquares = getPossibleMoves(true);
            int kingPosition;
            for(short moves : moveSquares){
                // The moves are currently pseudo-legal, test if king is in check to get legal moves
                Move movement = new Move(board, moves);
                movement.makeMove();    // make the move on the board without making a copy
                kingPosition = board.getKingPosition(this.isWhite());
                // if king is not under check after making the move, the move is legal.
                if(!board.isTileAttacked(kingPosition, this.isWhite())) {
                    movement.unMake();
                    if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                        generatePawnPromotionMoves(moves, moveList);
                    }
                    else{
                        moveList.add(moves);
                    }
                }
                else{
                    movement.unMake();  // revert board back to its original state
                }
            }
        }
        return moveList;
    }

    public final boolean isPinned(){
        return board.isPinned(getPosition());
    }

    public final boolean isSliderPiece(){
        return isQueen() || isRook() || isBishop();
    }

    /**
     * Gets the side which the piece is on (White / Black)
     * @return true if piece is white, else return false if piece is black
     */
    public final boolean isWhite(){
        return colour;
    }

    /**
     * @return the current position of the piece on the chess board
     */
    public final int getPosition() {
        return position;
    }

    /**
     * Sets position of the piece on the board once it moves or is placed
     * @param position refers to the new position of the piece
     */
    public final void setPosition(int position){
        this.position = position;
    }

    /**
     * Getter function to obtain which row the current board position index lies on the board
     * @param position refers to the index of a tile on the board
     * @return the row which the index is on (i.e. index 8 lies on row 1)
     */
    public static int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    /**
     * Getter function to obtain which column the current board position index lies on the board
     * @param position refers to the index of a tile on the board
     * @return the column which the index is on (i.e. index 8 lies on column 0)
     */
    public static int getCol(int position){
        return position % 8;
    }

    /**
     * @return the type of the piece
     */
    public PieceType getType(){
        return type;
    }

    /**
     * Checks if a piece is a pawn.
     * @return true if the piece is a pawn else return false
     */
    public boolean isPawn(){
        return this.type == PieceType.PAWN;
    }

    /**
     * Checks if a piece is a knight.
     * @return true if the piece is a knight else return false
     */
    public boolean isKnight(){
        return this.type == PieceType.KNIGHT;
    }

    /**
     * Checks if a piece is a bishop.
     * @return true if the piece is a bishop else return false
     */
    public boolean isBishop(){
        return this.type == PieceType.BISHOP;
    }

    /**
     * Checks if a piece is a rook.
     * @return true if the piece is a rook else return false
     */
    public boolean isRook(){
        return this.type == PieceType.ROOK;
    }

    /**
     * Checks if a piece is a queen.
     * @return true if the piece is a queen else return false
     */
    public boolean isQueen(){
        return this.type == PieceType.QUEEN;
    }

    /**
     * Checks if a piece is a king.
     * @return true if the piece is a king else return false
     */
    public boolean isKing(){
        return this.type == PieceType.KING;
    }

    public static char getFile(int position){
        int col = getCol(position);
        return (char) ('a' + col);
    }

    public static char getRank(int position){
        int rank = 8 - getRow(position);
        return (char) (rank + '0'); // convert int to a char
    }

    //---------------------------------------------------//
    /* ABSTRACT CLASSES TO BE IMPLEMENTED BY SUB CLASSES */
    //---------------------------------------------------//


    /**
     * Obtain the pseudo-legal moves of a piece
     * @return a list of all pseudo-legal moves of the piece
     */
    public abstract ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly);

    /**
     * Gets abbreviation of piece name
     * Abbreviations:
     *      1) King -> 'K'
     *      2) Queen -> 'Q'
     *      3) Rook -> 'R'
     *      4) Bishop -> 'B'
     *      5) Knight -> 'N'
     *      6) Pawn -> 'P'
     * @return the abbreviated name of piece
     */
    public abstract String toString();

    /**
     * Represents the total value of a piece including position evaluation
     * > Used for Chess Engine score evaluation
     * @return the value of the chess piece
     */
    public abstract int getValue();

    /**
     * @return the value of the piece itself without positional evaluation
     */
    public abstract int getPieceValue();
}
