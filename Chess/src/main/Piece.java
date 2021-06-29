import java.util.ArrayList;

public abstract class Piece {
    protected final boolean colour; // White == 1, Black == 0;
    protected int position; // position of piece on board
    protected Board board; // reference to the chess board

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
    public ArrayList<Integer> getLegalMoves(){
        ArrayList<Integer> moveList = new ArrayList<>();
        ArrayList<Integer> defendingSquares = getDefendingSquares();

        for(int move : defendingSquares){
            if(board.getTile(move).isOccupied()){   // check if piece is attacking / defending another piece
                // filter out allied pieces as it cannot move to tiles occupied by allied pieces
                if(board.getTile(move).getPiece().isWhite() == this.isWhite()) {
                    continue;
                }
            }
            /*
             *  At this point, the move will become a pseudo-legal move.
             *  test the pseudo-legal move to check if king is under check after moving any piece
             */

            Move movement = new Move(this.board, this.position, move);
            movement.makeMove();    // make the move on the board without making a copy
            // if king is not under check after making the move, the move is legal.
            if(!this.board.isKingChecked(this.isWhite())) {
                moveList.add(move);
            }
            movement.unMake();  // revert board back to its original state
        }
        return moveList;
    }

    /**
     * Checks that a move (from this.position to end)being made is a legal move
     * @param endPosition refers to the position piece is moving to
     * @return true if the move is generated in getLegalMoves() method, else return false
     */
    public boolean isLegalMove(int endPosition){
        for(int legalMoves : getLegalMoves()){
            if(legalMoves == endPosition){
                return true;
            }
        }
        return false;
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
    public int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    /**
     * Getter function to obtain which column the current board position index lies on the board
     * @param position refers to the index of a tile on the board
     * @return the column which the index is on (i.e. index 8 lies on column 0)
     */
    public int getCol(int position){
        return position % 8;
    }


    /* ABSTRACT CLASSES TO BE IMPLEMENTED BY SUB CLASSES */


    /**
     * Obtain the squares which a piece has control regardless if it is defending allied pieces
     * or attacking opposing pieces
     * @return a list of all defending squares of a piece (squares which piece controls)
     */
    public abstract ArrayList<Integer> getDefendingSquares();

    /**
     * Checks if a move is within boundaries and possible (regardless if there is a piece at end position)
     * @param start refers to the starting position of the piece
     * @param end refers to the end position of the piece after moving
     * @return true if the move is possible, else return false
     */
    public abstract boolean isValidMove(int start, int end);

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
