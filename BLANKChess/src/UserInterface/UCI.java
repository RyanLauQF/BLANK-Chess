import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UCI {
    public static final String ENGINE_NAME = "BLANK";
    public static final String VERSION = "v1.0.1";
    public static final String AUTHOR = "Ryan Lau Q. F.";
    public static final int INFINITE_SEARCH = Integer.MAX_VALUE;
    public static final int DEFAULT_SEARCH = 5; // default search duration set to 5 seconds per search
    public static String FEN = FENUtilities.startFEN;
    public static boolean usingOwnBook = true;

    public Board board;
    public AI engine;


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
                board = new Board();
                board.init(FEN);
                engine = new AI(board.isWhiteTurn(), board);

                System.out.println("id name " + ENGINE_NAME + " " + VERSION);
                System.out.println("id author " + AUTHOR);
                System.out.println("\noption name OwnBook type check default true\n");
                System.out.println("uciok");

                // starts UCI communication
                UCICommunicate();
            }
        }
    }

    /**
     * Implements the UCI protocol (Not all functions are available)
     */
    public void UCICommunicate() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while(true){
            // obtains UCI command from CLI
            String command = reader.readLine();

            // tells the engine to use UCI protocol. Engine will identify itself and respond with uciok
            if (command.equals("uci")) {
                System.out.println("id name " + ENGINE_NAME + " " + VERSION);
                System.out.println("id author " + AUTHOR);
                System.out.println("\noption name OwnBook type check default true\n");
                System.out.println("uciok");
            }

            // used by GUI to check if engine is responding
            else if (command.equals("isready")) {
                System.out.println("readyok");
            }

            else if (command.startsWith("setoption name ")){
                processOption(command);
            }

            // prints out the starting FEN of the board
            else if (command.equals("fen")) {
                System.out.println(FEN);
            }

            // re-initialise the board and engine
            else if (command.equals("ucinewgame")) {
                board = new Board();
                board.init(FENUtilities.startFEN);
                engine = new AI(board.isWhiteTurn(), board);
            }

            // sets up a fen position on the board
            else if (command.startsWith("position")) {
                parsePosition(command);
                engine.board.state();
            }

            // starts search
            else if (command.startsWith("go")) {
                processGo(command);
            }

            // prints out the board
            else if (command.equals("print")) {
                board.state();
            }

            else if (command.contains("stop")) {
                //** Not yet working **//
                // reset the chess board
                board = new Board();
                board.init(FEN);
            }

            else if (command.equals("help")) {
                System.out.println("Usage:");
                System.out.println("- go <movetime> <time in seconds>");
                System.out.println("- go wtime <wtime> btime <btime> winc <winc> binc <binc>");
                System.out.println("- go perft <depth>");
            }

            // uses local GUI
            else if (command.equals("gui")) {
                break;
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

    private void processOption(String input){
        // cut "setoption name " out of the input
        input = input.substring(15);
        if(input.startsWith("OwnBook value ")){
            // setoption name OwnBook value false
            input = input.substring(input.lastIndexOf("value") + 5);
            if(input.contains("true")){
                usingOwnBook = true;
                System.out.println("OwnBook enabled!");
            }
            else if(input.contains("false")){
                usingOwnBook = false;
                System.out.println("OwnBook disabled!");
            }
        }
    }

    private void processGo(String input) {
        double ALLOCATED_TIME = DEFAULT_SEARCH;
        double TOTAL_TIME_LEFT = 0;
        double INCREMENT_TIME = 0;

        boolean isWhite = engine.isWhite();
        boolean useOpeningBook = engine.isUsingOpeningBook;
        boolean isPerft = false;
        boolean defaultTiming = false;

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
                    engine.searchMove(useOpeningBook && usingOwnBook, ALLOCATED_TIME);
                    return;
                case "infinite":
                    TOTAL_TIME_LEFT = INFINITE_SEARCH;
                    break;
                case "perft":
                    int depth = Integer.parseInt(tokens[index + 1]);
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
        engine.searchMove(useOpeningBook && usingOwnBook, ALLOCATED_TIME);
    }

    // debugging uci commands
    // position startpos moves e2e4 g8f6 e4e5 f6d5 c2c4 d5b6 b2b3 g7g6 c1b2 f8g7 g1f3 d7d6 f1e2 c7c5 d2d4 c5d4
    // position fen "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1" moves e2a6 b4c3

    private void parsePosition(String input) throws IOException {
        boolean isWhiteTurn = true;

        // check if after making all the moves if the engine is playing white or black move
        String moveString = "";
        if(input.contains("moves")){    // cut the string before the moves are processed
            moveString = input.substring(input.indexOf("moves"));
            String[] moves = moveString.split(" ");
            if((moves.length - 1) % 2 != 0){
                isWhiteTurn = false;
            }
        }

        // create the board based on the initial fen input
        board = new Board();
        boolean enableOpeningBook = false;
        if(input.startsWith("position startpos")){
            board.init(FENUtilities.startFEN);
            enableOpeningBook = true;
        }
        else if(input.startsWith("position fen ")){
            // custom fen input
            String fen = input.substring(13);
            if(input.contains("moves")){    // cut the string before the moves are processed
                fen = fen.substring(0, fen.indexOf("moves"));
            }
            FEN = fen.replaceAll("\"", "");
            board.init(FEN);
        }

        // create the AI based on which turn to play and the starting board state
        engine = new AI(isWhiteTurn, board);
        engine.isUsingOpeningBook = enableOpeningBook;
        // process moves
        if(input.contains("moves")){
            // moves[0] will be "moves";
            // index 1 onwards will be the individual moves made.
            String[] moves = moveString.split(" ");
            isWhiteTurn = board.isWhiteTurn();
            boolean moveExistsInBook = false;
            String lastMove = "";
            Move previousMove;
            board.setPreviousMove(null);

            if(moves.length >= 16 || !usingOwnBook) {    // disable using opening book if the moves have exceeded the length of opening book
                engine.isUsingOpeningBook = false;
            }

            for (int i = 1; i < moves.length; i++) {
                // get start and end index and determine the move type of the move
                int startPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(0, 2));
                int endPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(2, 4));

                short currentMove = MoveGenerator.generateMove(startPosition, endPosition, determineMoveType(moves[i], board));

                // records the moves made in the engine's opening book
                if(engine.isUsingOpeningBook && i != moves.length - 1){
//                    if(previousMove != null){
//                        System.out.println("Previous Move: " + MoveGenerator.toString(board.getPreviousMove().getEncodedMove()));
//                    }
                    // position startpos moves g1f3 g8f6 e2e3 g7g6 f1d3 d7d5 b1c3

                    for(String bookMoves : engine.openingBook.getSetOfBookMoves()){
                        if(PGNExtract.convertNotationToMove(board, isWhiteTurn, bookMoves) == currentMove){
                            moveExistsInBook = true;
                            lastMove = bookMoves;   // get the PGN String format of the move
                            break;
                        }
                    }

                    if(moveExistsInBook){
                        // update the opening book
                        boolean bookWasUpdated = engine.openingBook.makeMove(lastMove);
                        if(!bookWasUpdated){
                            engine.isUsingOpeningBook = false;
                        }
                        else{
                            System.out.println("Move recorded in Book: " + lastMove);
                        }
                    }
                    else{
                        engine.isUsingOpeningBook = false;
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
