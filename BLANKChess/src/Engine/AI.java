import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class AI {
    protected final boolean isWhite;
    protected Board board;
    public final OpeningTrie openingBook;  // opening book used by AI
    public final Search searcher;

    public int moveNum;
    public boolean isUsingOpeningBook;

    public AI(boolean isWhite, Board board) throws IOException {
        this.isWhite = isWhite;
        this.board = board;
        this.openingBook = new OpeningTrie(isWhite);    // builds the opening book for the AI
        this.isUsingOpeningBook = true;
        this.moveNum = 0;  // number of moves made by this AI
        this.searcher = new Search(board);
    }

    public boolean isWhite(){
        return isWhite;
    }

    public void setBoard(Board board){
        this.board = board;
    }

    // selects a random move when it is the AI turn
    public short getRandomMove(){
        ArrayList<Short> moves = board.getAllLegalMoves();
        Random rand = new Random();
        int randomMove = rand.nextInt(moves.size());
        short move = moves.get(randomMove);
        System.out.println("Making Random Move!");
        System.out.println("Random move: " + MoveGenerator.toString(move));
        return move;
    }

    public short searchMove(boolean enableOpeningBook, double searchDuration, boolean enableThreadedSearch) {
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

        // Use threaded Search ONLY when in UCI mode (connected to external gui)
        if(enableThreadedSearch){
            // threaded search will only print "bestmove" once it has ended or has been killed
            // hence it will not return a bestmove
            searcher.startThreadedSearch(searchDuration);
            return 0;
        }
        else{
            // use standard search in local gui
            //bestMove = searcher.startSearch(searchDuration);
            searcher.startThreadedSearch(searchDuration);
            try{
                // wait for search to complete before continuing
                searcher.searchThread.join();
            }
            catch(InterruptedException e){
                System.out.println("Search was incomplete!");
            }
            bestMove = searcher.bestMoveFound[0];
        }
        return bestMove;
    }

    public void stopSearcher(){
        searcher.stopSearch();
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

    /**
     * Unit Testing
     */
    public static void main(String[] args) throws IOException {
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);
        AI testAI = new AI(false, board);

        int timePerSearch = 15;
        testAI.searchMove(false, timePerSearch, true);
    }
}
