import java.io.IOException;

public class EngineMain {
    public OpeningTrie whiteOpeningBook;
    public OpeningTrie blackOpeningBook;

    public Board board;
    public final Search searcher;
    public final TranspositionTable TT;

    public boolean openingBooksLoaded;
    public boolean isUsingWhiteBook;
    public boolean isUsingBlackBook;

    public EngineMain(Board board, boolean loadOpeningBook) throws IOException {
        this.board = board;
        this.openingBooksLoaded = loadOpeningBook;
        this.isUsingWhiteBook = openingBooksLoaded;
        this.isUsingBlackBook = openingBooksLoaded;

        this.TT = new TranspositionTable();
        this.searcher = new Search(board, TT);

        // Check if in-built opening book can be created
        if(loadOpeningBook){
            // builds the opening books used by engine
            this.whiteOpeningBook = new OpeningTrie(true);
            this.blackOpeningBook = new OpeningTrie(false);
        }
    }

    public short searchMove(boolean enableOpeningBook, double searchDuration){
        short bestMove, whiteBookBestMove, blackBookBestMove;

        // gets opening moves from opening book for the first few moves (up to 8)
        if(board.getFullMoveNum() <= 8 && openingBooksLoaded && enableOpeningBook){
            whiteBookBestMove = getOpeningMove(whiteOpeningBook);
            blackBookBestMove = getOpeningMove(blackOpeningBook);

            bestMove = board.isWhiteTurn() ? whiteBookBestMove : blackBookBestMove;

            // checks if the opening book contains the move, if it does not, -1 is returned
            if(bestMove != -1){
                System.out.println("Using Opening Book!");
                System.out.println("bestmove " + MoveGenerator.toString(bestMove));
                return bestMove;
            }
        }

        searcher.setBoard(board);
        bestMove = searcher.startSearch(searchDuration);
        return bestMove;
    }

    public short fixedDepthSearch(int depth){
        searcher.setBoard(board);
        return searcher.depthSearch(depth);
    }

    public short getOpeningMove(OpeningTrie openingBook){
        Move previousMove = board.getPreviousMove();
        short openingMove = -1; // return -1 if the opening move is not found

        if(!openingBook.hasMoves()){
            OpeningTrie.SIDE openingBookSide = openingBook.getSide();
            if(openingBookSide == OpeningTrie.SIDE.WHITE){
                isUsingWhiteBook = false;
            }
            else if(openingBookSide == OpeningTrie.SIDE.BLACK){
                isUsingBlackBook = false;
            }
            return openingMove;
        }

        if(previousMove == null){   // first move being made. opening book does not need to record opponent move
            openingMove = getMoveFromOpeningBook(openingBook);
            return openingMove;
        }
        else{
            // record previous move made in opening books
            short previousMoveMade = previousMove.getEncodedMove();
            boolean moveExistsInBook = false;
            String lastMove = null;
            // check if the previous move made by opponent exists in opening book
            previousMove.unMake();  // undo previous move and check
            for(String bookMoves : openingBook.getSetOfBookMoves()){
                if(PGNExtract.convertNotationToMove(board, board.isWhiteTurn(), bookMoves) == previousMoveMade){
                    moveExistsInBook = true;
                    lastMove = bookMoves;   // get the PGN String format of the move
                    break;
                }
            }
            previousMove.makeMove();  // make the move again
            if(moveExistsInBook){
                // update the opening book with previous opponent move and get a response
                openingBook.makeMove(lastMove);
                openingMove = getMoveFromOpeningBook(openingBook);
                return openingMove;
            }
            else{
                if(board.isWhiteTurn()){
                    isUsingWhiteBook = false;
                }
                else{
                    isUsingBlackBook= false;
                }
            }
        }
        return openingMove;
    }

    public short getMoveFromOpeningBook(OpeningTrie openingBook){
        // convert the move from algebraic notation to an encoded move.
        String PGN_notation = openingBook.getNextMove();
        openingBook.makeMove(PGN_notation); // make the AI's move in opening book
        return PGNExtract.convertNotationToMove(board, board.isWhiteTurn(), PGN_notation);
    }

    public void resetOpeningTrie(){
        if(!openingBooksLoaded) return;

        whiteOpeningBook.resetMoveTracker();
        blackOpeningBook.resetMoveTracker();

        isUsingWhiteBook = true;
        isUsingBlackBook = true;
    }

    public void disableBooks(){
        isUsingWhiteBook = false;
        isUsingBlackBook = false;
    }

    public void clearBooks(){
        // Clear away opening books occupying memory.
        whiteOpeningBook = null;
        blackOpeningBook = null;
        openingBooksLoaded = false;
        Runtime.getRuntime().gc();
    }

    public boolean isWhite(){
        return board.isWhiteTurn();
    }

    public void setBoard(Board board){
        this.board = board;
    }

    /**
     * Unit Testing
     */
    public static void main(String[] args) throws IOException {
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);
        EngineMain engineMain = new EngineMain(board, false);
        engineMain.resetOpeningTrie();
    }
}
