import java.io.IOException;

public class AI {
    protected final boolean isWhite;
    protected Board board;
    public OpeningTrie openingBook;  // opening book used by AI
    public final Search searcher;

    public int moveNum;
    public boolean isUsingOpeningBook;

    public AI(boolean isWhite, Board board){
        this.isWhite = isWhite;
        this.board = board;
        this.isUsingOpeningBook = true;
        this.moveNum = 0;  // number of moves made by this AI
        this.searcher = new Search(board);

        try{
            // builds the opening book for the AI
            this.openingBook = new OpeningTrie(isWhite);
        }
        catch (IOException exception){
            System.out.println("Unable to initialize opening book!");
        }
    }

    public boolean isWhite(){
        return isWhite;
    }

    public void setBoard(Board board){
        this.board = board;
    }

    public short searchMove(boolean enableOpeningBook, double searchDuration) {
        moveNum++;
        short bestMove;

        // gets opening moves from opening book for the first few moves (up to 8)
        if(this.moveNum <= 8 && isUsingOpeningBook && enableOpeningBook){
            bestMove = getOpeningMove();
            // checks if the opening book contains the move, if it does not, -1 is returned
            if(bestMove != -1){
                return bestMove;
            }
        }

        bestMove = searcher.startSearch(searchDuration);
        return bestMove;
    }

    public short getOpeningMove(){
        Move previousMove = board.getPreviousMove();
        short openingMove = -1; // return -1 if the opening move is not found

        if(previousMove == null){   // first move being made. AI opening book does not need to record opponent move
            openingMove = getMoveFromOpeningBook();
            System.out.println("Using Opening Book!");
            System.out.println("bestmove " + MoveGenerator.toString(openingMove));
            return openingMove;
        }
        else{
            short previousMoveMade = previousMove.getEncodedMove();
            boolean moveExistsInBook = false;
            String lastMove = null;
            // check if the previous move made by opponent exists in opening book
            previousMove.unMake();  // undo previous move and check
            for(String bookMoves : openingBook.getSetOfBookMoves()){
                if(PGNExtract.convertNotationToMove(board, !isWhite(), bookMoves) == previousMoveMade){
                    moveExistsInBook = true;
                    lastMove = bookMoves;   // get the PGN String format of the move
                    break;
                }
            }
            previousMove.makeMove();  // make the move again
            if(moveExistsInBook){
                // update the opening book with previous opponent move and get a response
                openingBook.makeMove(lastMove);
                openingMove = getMoveFromOpeningBook();
                System.out.println("Using Opening Book!");
                System.out.println("bestmove " + MoveGenerator.toString(openingMove));
                return openingMove;
            }
            else{
                isUsingOpeningBook = false;
            }
        }
        return openingMove;
    }

    public short getMoveFromOpeningBook(){
        // convert the move from algebraic notation to an encoded move.
        String PGN_notation = openingBook.getNextMove();
        openingBook.makeMove(PGN_notation); // make the AI's move in opening book
        return PGNExtract.convertNotationToMove(board, isWhite(), PGN_notation);
    }

    /*
     * Selects a random move to make
     */
//    public short getRandomMove(){
//        ArrayList<Short> moves = board.getAllLegalMoves();
//        Random rand = new Random();
//        int randomMove = rand.nextInt(moves.size());
//        short move = moves.get(randomMove);
//        System.out.println("Making Random Move!");
//        System.out.println("Random move: " + MoveGenerator.toString(move));
//        return move;
//    }

    /**
     * Unit Testing
     */
    public static void main(String[] args){
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);
        AI testAI = new AI(false, board);

        int timePerSearch = 15;
        testAI.searchMove(false, timePerSearch);
    }
}
