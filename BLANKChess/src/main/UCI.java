import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class UCI {
    public static final String ENGINE_NAME = "BLANK";
    public static final String VERSION = "v1.2.0";
    public static final String AUTHOR = "Ryan Lau Q. F.";
    public static final int INFINITE_SEARCH = Integer.MAX_VALUE;
    public static final int DEFAULT_SEARCH = 5; // default search duration set to 5 seconds per search
    public static String FEN = FENUtilities.startFEN;
    public static boolean loadOpeningBook = true;   // default will load opening book unless option is disabled

    public Board board;
    public EngineMain BLANK_ENGINE;

    public UCI() throws IOException {
        System.out.println("BLANK Chess Engine");
        System.out.println(VERSION);
        System.out.println("author " + AUTHOR + "\n");

        System.out.println("commands:");
        System.out.println("'uci' - to enable Universal Chess Interface (UCI protocol)");
        System.out.println("'gui' - to use local built-in gui");
        System.out.println("----------------------------------------------------------");

        // checks if user wants to enable uci
        while(true){
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String command = reader.readLine();

            if(command.equals("gui")){
                break;
            }
            else if(command.contains("quit")){
                System.exit(0);
            }
            else if(command.equals("uci")){
                // initiates board to starting FEN and initiates engine to turn of FEN
                initBoard(FEN);

                // create the chess engine
                BLANK_ENGINE = new EngineMain(board, loadOpeningBook);

                printInfo();

                // starts UCI communication
                UCICommunicate();
            }
        }
    }

    /**
     * Implements the UCI protocol
     */
    public void UCICommunicate() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while(true){
            // obtains UCI command from CLI
            String command = reader.readLine();

            // tells the engine to use UCI protocol. Engine will identify itself and respond with uciok
            if (command.equals("uci")) {
                printInfo();
            }

            // used by GUI to check if engine is responding
            else if (command.equals("isready")) {
                System.out.println("readyok");
            }

            else if (command.startsWith("setoption name ")){
                processOption(command);

                // re-initiate engine with updated parameters
                initBoard(FEN);
                BLANK_ENGINE = new EngineMain(board, loadOpeningBook);

                // garbage collection to clean up memory usage
                Runtime.getRuntime().gc();
            }

            // prints out the starting FEN of the board
            else if (command.equals("fen")) {
                System.out.println(FEN);
            }

            // re-initialise the board and engine
            else if (command.equals("ucinewgame")) {
                initBoard(FENUtilities.startFEN);
                BLANK_ENGINE = new EngineMain(board, loadOpeningBook);

                // free up memory usage from previous searches
                Runtime.getRuntime().gc();
            }

            // sets up a fen position on the board
            else if (command.startsWith("position")) {
                parsePosition(command);
                BLANK_ENGINE.board.print(false);
            }

            // starts search
            else if (command.startsWith("go")) {
                processGo(command);
                BLANK_ENGINE.resetOpeningTrie();
            }

            // prints out the board
            else if (command.equals("print")) {
                board.print(false);
            }

            else if (command.contains("stop")) {
                // stop function is built into engine search
                System.out.println("Stop only when search has started!");
            }

            else if (command.equals("help")) {
                System.out.println("Usage:");
                System.out.println("- go <movetime> <time in seconds>");
                System.out.println("- go wtime <wtime> btime <btime> winc <winc> binc <binc>");
                System.out.println("- go perft <depth>");
            }

            // quit the program
            else if (command.equals("quit")) {
                System.exit(0);
            }

            else {
                System.out.println("Unknown command");
            }
        }
    }

    /**
     * Prints the engine info and options when "uci" is called
     */
    private void printInfo(){
        System.out.println("id name " + ENGINE_NAME + " " + VERSION);
        System.out.println("id author " + AUTHOR);
        System.out.println("\noption name Hash type spin default 32 min 1 max 128");
        System.out.println("option name OwnBook type check default true\n");
        System.out.println("uciok");
    }

    /**
     * Initiates a new board with the new FEN
     * @param FEN refers to the standard chess notation to describe a particular chess board position
     */
    private void initBoard(String FEN){
        board = new Board();
        board.init(FEN);
        UCI.FEN = FEN;
    }

    /**
     * Takes in a UCI setoption command and applies relevant options to engine
     * @param input refers to the UCI command (i.e. setoption name Hash value 32)
     */
    private void processOption(String input){
        //setoption name Hash value 32
        String[] splitInput = input.split(" ");

        // Invalid input
        if(splitInput.length != 5){
            // all set options commands can be split into 5 parts
            // i.e. setoption name Hash value 32
            return;
        }

        if(splitInput[2].equals("OwnBook")){
            // i.e. setoption name OwnBook value false
            if(splitInput[4].equals("true")){
                loadOpeningBook = true;
                System.out.println("OwnBook enabled!");
            }
            else if(splitInput[4].equals("false")){
                loadOpeningBook = false;
                System.out.println("OwnBook disabled!");
            }
        }

        else if(splitInput[2].equals("Hash")){
            // setoption name Hash value 32 (in megabytes)
            int hashSize = Integer.parseInt(splitInput[4]);
            TranspositionTable.ALLOCATED_HASH_SIZE_MEGABYTES = hashSize;
            System.out.println("Allocated " + hashSize + " MB for hash table!");
        }
    }

    private void processGo(String input){
        double ALLOCATED_TIME = DEFAULT_SEARCH;
        double TOTAL_TIME_LEFT = 0;
        double INCREMENT_TIME = 0;

        boolean isWhite = BLANK_ENGINE.isWhite();
        boolean useOpeningBook;

        if(isWhite){
            useOpeningBook = BLANK_ENGINE.isUsingWhiteBook;
        }
        else{
            useOpeningBook = BLANK_ENGINE.isUsingBlackBook;
        }

        if(!Objects.equals(FEN, FENUtilities.startFEN)){
            useOpeningBook = false;
        }

        boolean isPerft = false;
        boolean defaultTiming = false;
        int depth;

        int index = 0;
        String[] tokens = input.split(" ");
        while(index < tokens.length){
            switch (tokens[index]) {
                case "wtime":
                    if(isWhite){
                        TOTAL_TIME_LEFT = Integer.parseInt(tokens[index + 1]);
                        index += 2;
                    }
                    break;

                case "btime":
                    if(!isWhite){
                        TOTAL_TIME_LEFT = Integer.parseInt(tokens[index + 1]);
                        index += 2;
                    }
                    break;

                case "winc":
                    if(isWhite){
                        INCREMENT_TIME = Integer.parseInt(tokens[index + 1]);
                        index += 2;
                    }
                    break;

                case "binc":
                    if(!isWhite){
                        INCREMENT_TIME = Integer.parseInt(tokens[index + 1]);
                        index += 2;
                    }
                    break;

                case "movetime":
                    // search for exactly input milliseconds
                    ALLOCATED_TIME = Integer.parseInt(tokens[index + 1]);
                    ALLOCATED_TIME /= 1000; // convert from milliseconds to seconds

                    BLANK_ENGINE.searchMove(useOpeningBook && loadOpeningBook, ALLOCATED_TIME);
                    return;

                case "infinite":
                    BLANK_ENGINE.searchMove(false, INFINITE_SEARCH);
                    /*
                     * MAKE SURE TO CALL 'ucinewgame' AFTER USING INFINITE SEARCH
                     */
                    return;

                case "depth":
                    depth = Integer.parseInt(tokens[index + 1]);
                    BLANK_ENGINE.fixedDepthSearch(depth);
                    return;

                case "perft":
                    depth = Integer.parseInt(tokens[index + 1]);
                    Perft goPerft = new Perft(board);
                    goPerft.perft(depth);
                    isPerft = true;
                    index += 2;
                    break;

                default:
                    break;
            }
            index++;
        }

        if(TOTAL_TIME_LEFT == 0 && INCREMENT_TIME == 0){
            defaultTiming = true;
        }

        if(!isPerft && !defaultTiming){
            // time given is in milliseconds, convert to seconds
            TOTAL_TIME_LEFT = TOTAL_TIME_LEFT / 1000;
            INCREMENT_TIME = INCREMENT_TIME / 1000;
            ALLOCATED_TIME = Clock.getTimePerMove(TOTAL_TIME_LEFT, INCREMENT_TIME);
        }

        // start searching with engine
        BLANK_ENGINE.searchMove(useOpeningBook && loadOpeningBook, ALLOCATED_TIME);
    }

    // debugging uci commands
    // position startpos moves e2e4 g8f6 e4e5 f6d5 c2c4 d5b6 b2b3 g7g6 c1b2 f8g7 g1f3 d7d6 f1e2 c7c5 d2d4 c5d4
    // position fen "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1" moves e2a6 b4c3

    private void parsePosition(String input){
        // create the board based on the initial fen input
        board = new Board();

        if(input.startsWith("position startpos")){
            board.init(FENUtilities.startFEN);
        }
        else if(input.startsWith("position fen ")){
            // custom fen input
            String fen = input.substring(13);
            if(input.contains("moves")){    // cut the string before the moves are processed
                fen = fen.substring(0, fen.indexOf("moves"));
            }
            FEN = fen.replaceAll("\"", "");
            try{
                board.init(FEN);
            }
            catch(IllegalArgumentException error){
                System.out.println("Invalid FEN!");
                board.init(FENUtilities.startFEN);
                FEN = FENUtilities.startFEN;
            }
        }

        // create the AI based on which turn to play and the starting board state
        //engine = new AI(isWhiteTurn, board, loadOpeningBook);

        // reset the tracker in the opening book and set to current board state
        if(loadOpeningBook){
            BLANK_ENGINE.resetOpeningTrie();
        }

        BLANK_ENGINE.setBoard(board);

        // process moves
        if(input.contains("moves")){
            // moves[0] will be "moves";
            // index 1 onwards will be the individual moves made.
            String moveString = input.substring(input.indexOf("moves"));
            String[] moves = moveString.split(" ");
            boolean isWhiteTurn = board.isWhiteTurn();
            boolean moveExistsInBook = false;
            String lastMove = "";
            Move previousMove;
            board.setPreviousMove(null);

            // disable using opening book if the moves have exceeded the length of opening book
            // or if opening book is not loaded
            if(moves.length >= 16 || !loadOpeningBook || !Objects.equals(FEN, FENUtilities.startFEN)) {
                BLANK_ENGINE.disableBooks();
                if(loadOpeningBook){
                    // if opening books are loaded, the engine will clear the book after the opening sequence
                    // or if the FEN of the current position is not equal to starting fen
                    BLANK_ENGINE.clearBooks();
                }
            }

            for (int i = 1; i < moves.length; i++) {
                // get start and end index and determine the move type of the move
                int startPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(0, 2));
                int endPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(2, 4));

                short currentMove = MoveGenerator.generateMove(startPosition, endPosition, determineMoveType(moves[i], board));

                // records the moves made in the engine's white opening book
                if(BLANK_ENGINE.isUsingWhiteBook && i != moves.length - 1){
                    for(String bookMoves : BLANK_ENGINE.whiteOpeningBook.getSetOfBookMoves()){
                        if(PGNExtract.convertNotationToMove(board, isWhiteTurn, bookMoves) == currentMove){
                            moveExistsInBook = true;
                            lastMove = bookMoves;   // get the PGN String format of the move
                            break;
                        }
                    }

                    if(moveExistsInBook){
                        // update the opening book
                        boolean bookWasUpdated = BLANK_ENGINE.whiteOpeningBook.makeMove(lastMove);
                        if(!bookWasUpdated){
                            BLANK_ENGINE.isUsingWhiteBook = false;
                        }
//                        else{
//                            System.out.println("Move recorded in White Book: " + lastMove);
//                        }
                    }
                    else{
                        BLANK_ENGINE.isUsingWhiteBook = false;
                    }
                }

                // records the moves made in the engine's black opening book
                if(BLANK_ENGINE.isUsingBlackBook && i != moves.length - 1){
                    for(String bookMoves : BLANK_ENGINE.blackOpeningBook.getSetOfBookMoves()){
                        if(PGNExtract.convertNotationToMove(board, isWhiteTurn, bookMoves) == currentMove){
                            moveExistsInBook = true;
                            lastMove = bookMoves;   // get the PGN String format of the move
                            break;
                        }
                    }

                    if(moveExistsInBook){
                        // update the opening book
                        boolean bookWasUpdated = BLANK_ENGINE.blackOpeningBook.makeMove(lastMove);
                        if(!bookWasUpdated){
                            BLANK_ENGINE.isUsingBlackBook = false;
                        }
//                        else{
//                            System.out.println("Move recorded in Black Book: " + lastMove);
//                        }
                    }
                    else{
                        BLANK_ENGINE.isUsingBlackBook = false;
                    }
                }

                // make the moves on the board and updates repetition history
                Move movement = new Move(board, currentMove);
                movement.makeMove();

                previousMove = movement;
                board.setPreviousMove(previousMove);

                long zobristHash = board.getZobristHash();
                int repetitionCount = board.repetitionHistory.containsKey(zobristHash) ? board.repetitionHistory.get(zobristHash) : 0;
                board.repetitionHistory.put(zobristHash, (byte) (repetitionCount + 1));

                isWhiteTurn = !isWhiteTurn;
            }
        }
    }


    /**
     * Takes the move information and processes it to get the move type needed to make the move on board
     * @param move refers to the move represented in algebraic notation i.e. e2a6, e7e8q.
     * @param board refers to the board
     * @return move type of the move as documented in MoveGenerator.java
     */
    private static int determineMoveType(String move, Board board) {
        int start = FENUtilities.convertRankAndFileToPosition(move.substring(0, 2));
        int end = FENUtilities.convertRankAndFileToPosition(move.substring(2, 4));

        char promotionType = 0;
        if (move.length() == 5) {
            promotionType = move.charAt(4);
        }

        int moveType = 0;
        if (board.getTile(start).isOccupied()) {
            Piece startPiece = board.getTile(start).getPiece();
            boolean isCapture = board.getTile(end).isOccupied();

            if (isCapture) {
                moveType = 4;
            }

            if (startPiece.isPawn()) {
                // double pawn push
                if (Math.abs(start - end) == 16) {
                    return 1;
                }
                // enpassant capture
                else if (board.getEnpassant() == end) {
                    return 5;
                }
                // promotion
                else if (Piece.getRow(end) == 0 || Piece.getRow(end) == 7) {
                    if (isCapture) {
                        // capture promotion
                        if (promotionType == 'q') {
                            moveType = 15;
                        } else if (promotionType == 'r') {
                            moveType = 14;
                        } else if (promotionType == 'b') {
                            moveType = 13;
                        } else if (promotionType == 'k') {
                            moveType = 12;
                        }
                    } else {
                        // normal promotion
                        if (promotionType == 'q') {
                            moveType = 11;
                        } else if (promotionType == 'r') {
                            moveType = 10;
                        } else if (promotionType == 'b') {
                            moveType = 9;
                        } else if (promotionType == 'k') {
                            moveType = 8;
                        }
                    }
                    return moveType;
                }
            }
            else if (startPiece.isKing()) {
                // check for castling move
                int diff = end - start;
                if (diff == 2) {
                    // king side castling, king position index increases by 2
                    return 2;
                } else if (diff == -2) {
                    // queen side castling, king position index decreases by 2
                    return 3;
                }
            }
        }
        return moveType;
    }

    public static void main(String[] args) throws IOException {
        new UCI();
    }
}
