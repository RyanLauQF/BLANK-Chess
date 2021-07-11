import java.util.ArrayList;

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
        // if piece is pinned, only generate the moves along pin axis
        if(board.isPinned(this.getPosition()) && !isPawn() && board.getCheckCount() == 0){
            if(this.isKnight()){
                return moveList;    // knight does not have any legal moves if it is pinned
            }
            int pinOffSet = board.getPinType(this.getPosition());
            if(Math.abs(pinOffSet) == 7 || Math.abs(pinOffSet) == 9){
                // diagonal pin
                if(isRook()){
                    return moveList;
                }
                else if(isBishop() || isQueen()){
                    return generatePinnedMoves(moveList, pinOffSet);
                }
            }
            else if(Math.abs(pinOffSet) == 1 || Math.abs(pinOffSet) == 8){
                // horizontal / vertical pin
                if(isBishop()){
                    return moveList;
                }
                else if(isRook() || isQueen()){
                    return generatePinnedMoves(moveList, pinOffSet);
                }
            }
        }
        else{
            ArrayList<Short> moveSquares = getPossibleMoves();
            int kingPosition, startPosition, endPosition;
            for(short moves : moveSquares){
                // The moves are currently pseudo-legal, test if king is in check to get legal moves
                Move movement = new Move(this.board, moves);
                movement.makeMove();    // make the move on the board without making a copy
                kingPosition = board.getKingPosition(this.isWhite());
                // if king is not under check after making the move, the move is legal.
                if(!board.isTileAttacked(kingPosition, this.isWhite())) {
                    movement.unMake();
                    if(this.isPawn() && Pawn.canPromote(this.isWhite(), MoveGenerator.getEnd(moves))){
                        // if the piece is a pawn and can be promoted, add promotion moves instead of normal move
                        startPosition = MoveGenerator.getStart(moves);
                        endPosition = MoveGenerator.getEnd(moves);
                        if(MoveGenerator.isCapture(moves)){     // generate capture-promotion moves
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

    private ArrayList<Short> generatePinnedMoves(ArrayList<Short> moveList, int pinOffSet){
        // generate moves towards the enemy piece by following pinned offset
        int endPosition = getPosition() + pinOffSet;
        while(!board.getTile(endPosition).isOccupied()){
            moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
            endPosition += pinOffSet;
        }
        moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 4));    // capture the enemy piece

        // reverse pinned offset to get offset towards king and generate moves towards the king
        endPosition = getPosition() - pinOffSet;
        while(!board.getTile(endPosition).isOccupied()){
            moveList.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
            endPosition -= pinOffSet;
        }
        return moveList;
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

    /* ABSTRACT CLASSES TO BE IMPLEMENTED BY SUB CLASSES */


    /**
     * Obtain the pseudo-legal moves of a piece
     * @return a list of all pseudo-legal moves of the piece
     */
    public abstract ArrayList<Short> getPossibleMoves();

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
     * Represents the value of a piece
     * > Used for Chess Engine score evaluation
     * @return the value of the chess piece
     */
    public abstract int getValue();
}
