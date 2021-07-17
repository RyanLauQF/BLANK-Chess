import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class ChessGUI extends JPanel {
    // TILE COLOURS
    private static final Color DARK_SQUARE_COLOUR = new Color(182, 136, 96);
    private static final Color LIGHT_SQUARE_COLOUR = new Color(241, 218, 179);
    private static final Color SELECTED_SQUARE_COLOUR = new Color(187, 203, 61);
    private static final Color LIGHT_MOVE_SQUARE_COLOUR = new Color(226, 81, 76);
    private static final Color DARK_MOVE_SQUARE_COLOUR = new Color(215, 72, 64);
    private static final Color LIGHT_MOVEMENT_HIGHLIGHT = new Color(253, 241, 149);
    private static final Color DARK_MOVEMENT_HIGHLIGHT = new Color(253, 241, 112);

    private final Stack<Integer> highlightedTiles;   // keeps track of highlighted tiles to be reset

    private final Board board;    // reference to the current board in chess game
    private boolean hasPieceBeenSelected;   // checks if piece is selected
    private int pieceSelected;  // index of selected piece, if not selected, set to -1
    private int prevHighlightedStart;   // index of highlighted previous start move tiles
    private int prevHighlightedEnd;     // index of highlighted previous end move tile


    /**
     * Constructor creates a overall JPanel to represent the chess board user interface
     * @param board refers to the chess board represented by a Board object
     */
    public ChessGUI(Board board){
        this.board = board;
        this.hasPieceBeenSelected = false;
        this.pieceSelected = -1;
        this.highlightedTiles = new Stack<>();
        this.prevHighlightedStart = -1;
        this.prevHighlightedEnd = -1;

        Dimension dimension = new Dimension(64, 64);
        setLayout(new GridLayout(8, 8));
        // create Tile panels
        // initialise colours of the tiles on the board
        for (int index = 0; index < 64; index++) {
            TilePanel tilePanel = new TilePanel(index, this);
            tilePanel.setPreferredSize(dimension);
            tilePanel.setMinimumSize(dimension);

            // set tiles to checkered colour
            tilePanel.setBackground(this.getTileOriginalColor(index));

            // Add piece to a tile if the tile is occupied
            if(board.getTile(index).isOccupied()){
                tilePanel.setPiece(board.getTile(index).getPiece());
            }
            else{
                tilePanel.setPiece(null);
            }
            add(tilePanel);
        }
    }

    /**
     * Updates the tile panels once a move is made on the board by copying entire board state
     * into the ChessGUI JPanel and updating the individual components
     */
    public void update() throws InterruptedException {
        int index = 0;
        for(Component component : this.getComponents()){
            if(component instanceof TilePanel){
                TilePanel currPanel = (TilePanel) component;
                currPanel.removeAll();
                currPanel.setBackground(getTileOriginalColor(index));
                if (board.getTile(index).isOccupied()) {
                    currPanel.setPiece(board.getTile(index).getPiece());
                } else {
                    currPanel.setPiece(null);
                }
                index++;
            }
        }
        this.revalidate();
        this.repaint();
        setHasPieceBeenSelected(false);
        setSelectedPiece(-1);
        TimeUnit.MICROSECONDS.sleep(1);
    }

    /**
     * Highlights a move (both start and end positions) when a move is made.
     * @param startPosition refers to the start position of the move
     * @param endPosition refers to the end position of the move
     */
    public void showMovementTiles(int startPosition, int endPosition){
        Component[] components = this.getComponents();
        if(components[startPosition] instanceof TilePanel){
            TilePanel currPanel = (TilePanel) components[startPosition];
            if(currPanel.getBackground() == LIGHT_SQUARE_COLOUR){
                currPanel.setBackground(LIGHT_MOVEMENT_HIGHLIGHT);
            }
            else if(currPanel.getBackground() == DARK_SQUARE_COLOUR){
                currPanel.setBackground(DARK_MOVEMENT_HIGHLIGHT);
            }
        }
        if(components[endPosition] instanceof TilePanel){
            TilePanel currPanel = (TilePanel) components[endPosition];
            if(currPanel.getBackground() == LIGHT_SQUARE_COLOUR){
                currPanel.setBackground(LIGHT_MOVEMENT_HIGHLIGHT);
            }
            else if(currPanel.getBackground() == DARK_SQUARE_COLOUR){
                currPanel.setBackground(DARK_MOVEMENT_HIGHLIGHT);
            }
        }
        // Stores the movement in order to keep movement tiles highlighted
        prevHighlightedStart = startPosition;
        prevHighlightedEnd = endPosition;
    }

    /**
     * Highlights all legal tiles that a piece is able to move to when a piece is selected
     */
    public void showLegalTiles(){
        // Get individual JPanels from ChessGUI JPanel
        Component[] components = this.getComponents();
        // Highlight tiles that piece is able to move to
        if(isPieceSelected()){
            for(short legalMoves : board.getTile(getSelectedPiece()).getPiece().getLegalMoves()){
                int endPosition = MoveGenerator.getEnd(legalMoves);
                if(components[endPosition] instanceof TilePanel){
                    // change background colour of legal move tiles
                    TilePanel currPanel = (TilePanel) components[endPosition];
                    Color originalColour = getTileOriginalColor(endPosition);
                    if(originalColour == LIGHT_SQUARE_COLOUR){
                        currPanel.setBackground(LIGHT_MOVE_SQUARE_COLOUR);
                    }
                    else if(originalColour == DARK_SQUARE_COLOUR){
                        currPanel.setBackground(DARK_MOVE_SQUARE_COLOUR);
                    }
                    highlightedTiles.add(endPosition);
                }
            }
        }
    }

    /**
     * Selects a piece if user clicks on it and highlights the legal moves of the piece
     * @param tilePosition refers to the position of the selected piece
     */
    public void select(int tilePosition) {
        setSelectedPiece(tilePosition);
        setHasPieceBeenSelected(true);
        // Get individual JPanels from ChessGUI JPanel
        Component[] components = this.getComponents();
        // Highlight selected tile
        components[tilePosition].setBackground(SELECTED_SQUARE_COLOUR);
        // Show all legal tiles of the selected piece
        showLegalTiles();
    }

    /**
     * Deselects the piece user has previously selected and resets the background colours
     */
    public void deselect(){
        // reset highlighted tiles
        resetHighlightedTiles();

        // deselect the previous selected piece
        Component[] components = this.getComponents();
        components[getSelectedPiece()].setBackground(getTileOriginalColor(getSelectedPiece()));
        setSelectedPiece(-1);
        setHasPieceBeenSelected(false);
    }

    /**
     * Reset all highlighted tiles to default colour
     */
    private void resetHighlightedTiles(){
        Component[] components = this.getComponents();
        int tileToBeReset;
        while(!highlightedTiles.isEmpty()){
            tileToBeReset = highlightedTiles.pop();
            if(tileToBeReset == prevHighlightedStart || tileToBeReset == prevHighlightedEnd){
                // keep the previous movement tiles highlighted
                Color originalColour = getTileOriginalColor(tileToBeReset);
                if(originalColour == LIGHT_SQUARE_COLOUR){
                    components[tileToBeReset].setBackground(LIGHT_MOVEMENT_HIGHLIGHT);
                }
                else if(originalColour == DARK_SQUARE_COLOUR){
                    components[tileToBeReset].setBackground(DARK_MOVEMENT_HIGHLIGHT);
                }
            }
            else{
                components[tileToBeReset].setBackground(getTileOriginalColor(tileToBeReset));
            }
        }
    }

    /**
     * Gets the original dark / light square colours of a tile based on the tiles position
     * @param position refers to the tiles position
     * @return the Color of the dark / light squares
     */
    private Color getTileOriginalColor(int position){
        // row and column of the tile
        int row = (position - (position % 8)) / 8;
        int col = position % 8;

        if((row + col + 1) % 2 == 0){
            return DARK_SQUARE_COLOUR; // dark squares
        }
        else {
            return LIGHT_SQUARE_COLOUR; // light squares
        }
    }

    /**
     * @return true if a user has selected a piece, else return false
     */
    public boolean isPieceSelected(){
        return hasPieceBeenSelected;
    }

    /**
     * @param isSelected refers to whether a piece has been selected by the user
     */
    public void setHasPieceBeenSelected(boolean isSelected){
        this.hasPieceBeenSelected = isSelected;
    }

    /**
     * @return the position of the piece selected by user
     */
    public int getSelectedPiece(){
        return pieceSelected;
    }

    /**
     * @param index refers position of the piece selected by user
     */
    public void setSelectedPiece(int index){
        this.pieceSelected = index;
    }

    /**
     * Creates a JFrame to store the ChessGUI JPanel and add the JPanel into the JFrame
     * ChessGUI JPanel stores 64 TilePanels (to represent the chess board)
     */
    public void initGUI(){
        JFrame frame = new JFrame("Chess Interface");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    /**
     * TilePanel is a JPanel nested within the overall ChessGUI JPanel.
     * It processes the pieces to show and the user inputs
     */
    public static class TilePanel extends JPanel {
        private final int position;
        private Piece piece;

        // Image icons of all white pieces
        private final ImageIcon whitePawn = getImage("/whitePawn.png");
        private final ImageIcon whiteBishop = getImage("/whiteBishop.png");
        private final ImageIcon whiteKnight = getImage("/whiteKnight.png");
        private final ImageIcon whiteRook = getImage("/whiteRook.png");
        private final ImageIcon whiteQueen = getImage("/whiteQueen.png");
        private final ImageIcon whiteKing = getImage("/whiteKing.png");

        // Image icons of all black pieces
        private final ImageIcon blackPawn = getImage("/blackPawn.png");
        private final ImageIcon blackBishop = getImage("/blackBishop.png");
        private final ImageIcon blackKnight = getImage("/blackKnight.png");
        private final ImageIcon blackRook = getImage("/blackRook.png");
        private final ImageIcon blackQueen = getImage("/blackQueen.png");
        private final ImageIcon blackKing = getImage("/blackKing.png");

        /**
         * Constructor to initialize all TilePanels. Each tile panel will have a mouse listener to get user input
         * on click to move the pieces
         * @param position refers to a position of a tilePanel which is final
         * @param gui refers to a reference to the overall ChessGUI JPanel
         */
        TilePanel(final int position, ChessGUI gui){
            super();
            this.position = position;

            // Add mouse listener to tile
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // if no piece is selected and tile is not occupied, do nothing
                    if(!isOccupied() && !gui.isPieceSelected()){
                        return;
                    }

                    // if no piece has been selected but tile is occupied, select the current piece to attack
                    if(isOccupied() && !gui.isPieceSelected()){
                        if(gui.board.getTile(getPosition()).getPiece().isWhite() != gui.board.isWhiteTurn()){
                            System.out.println("Wrong Side Piece");
                            return;
                        }
                        // select piece only if it is same as current turn (i.e. white turn, select white piece)
                        System.out.println("Selected");
                        gui.select(getPosition());
                    }
                    // else if a piece has been selected check if it is a legal move
                    else{
                        // check if the move being made is a legal move
                        boolean isLegal = false;
                        short currentMove = 0;
                        int selectedPiece = gui.getSelectedPiece();
                        for(short moves : gui.board.getTile(selectedPiece).getPiece().getLegalMoves()){
                            int end = MoveGenerator.getEnd(moves);
                            if(end == getPosition()){
                                isLegal = true;
                                currentMove = moves;
                                break;
                            }
                        }
                        if(!isLegal){   // if not a legal move, deselect the first move and return
                            gui.deselect();
                            return;
                        }

                        int moveType = MoveGenerator.getMoveType(currentMove);
                        int moveStart = gui.getSelectedPiece();
                        int moveEnd = getPosition();

                        Move move = new Move(gui.board, MoveGenerator.generateMove(moveStart, moveEnd, moveType));
                        // if the move is legal, make the move on the board
                        move.makeMove();
                        // Get user to choose promotion type if the move made is a promotion move
                        if(MoveGenerator.isPromotion(currentMove)){
                            JPanel panel = new JPanel();
                            panel.add(new JLabel("Select Promotion:"));
                            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                            model.addElement("Queen");
                            model.addElement("Knight");
                            model.addElement("Rook");
                            model.addElement("Bishop");
                            JComboBox<String> comboBox = new JComboBox<>(model);
                            panel.add(comboBox);

                            int result = JOptionPane.showConfirmDialog(null, panel, "Promotion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            String promotePiece = "Queen"; // default promotion set to queen if user closes option pane
                            if (result == JOptionPane.OK_OPTION) {
                                promotePiece = (String) comboBox.getSelectedItem();
                            }
                            assert promotePiece != null;
                            switch (promotePiece) {
                                case "Knight":
                                    gui.board.promote(Piece.PieceType.KNIGHT, gui.board.getTile(getPosition()));
                                    break;
                                case "Rook":
                                    gui.board.promote(Piece.PieceType.ROOK, gui.board.getTile(getPosition()));
                                    break;
                                case "Bishop":
                                    gui.board.promote(Piece.PieceType.BISHOP, gui.board.getTile(getPosition()));
                                    break;
                                default:
                                    gui.board.promote(Piece.PieceType.QUEEN, gui.board.getTile(getPosition()));
                                    break;
                            }
                            System.out.println("Promoted to a " + promotePiece + "!");
                        }
                        // update board tiles
                        gui.deselect();
                        try {
                            gui.update();
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        gui.showMovementTiles(moveStart, moveEnd);
                        // if Player vs AI disable game end check every move
//                        if(GameStatus.checkGameEnded(gui.board)){
//                            String gameState = GameStatus.getHowGameEnded();
//                            JOptionPane.showMessageDialog(gui, gameState,
//                                    "Game Manager", JOptionPane.INFORMATION_MESSAGE);
//                        }
                    }
                }
            });
        }

        public void setPiece(Piece piece) {
            this.piece = piece;
            JLabel pieceIcon = new JLabel(getPieceIcon(), SwingConstants.CENTER);
            this.add(pieceIcon);
        }

        private ImageIcon getImage(String path)
        {
            URL url = getClass().getResource(path);
            if (url != null)
                return (new ImageIcon(url));
            return null;
        }

        private ImageIcon getPieceIcon(){
            if(!this.isOccupied()){
                return null;
            }
            ImageIcon pieceIcon = new ImageIcon();
            String pieceToString = getPiece().toString();
            if(getPiece().isWhite()){
                switch (pieceToString) {
                    case "P":
                        // pieceIcon = ChessGUI.whitePawn;
                        pieceIcon = whitePawn;
                        break;
                    case "B":
                        pieceIcon = whiteBishop;
                        break;
                    case "N":
                        pieceIcon = whiteKnight;
                        break;
                    case "R":
                        pieceIcon = whiteRook;
                        break;
                    case "Q":
                        pieceIcon = whiteQueen;
                        break;
                    case "K":
                        pieceIcon = whiteKing;
                        break;
                }
            }
            else{
                switch (pieceToString) {
                    case "P":
                        pieceIcon = blackPawn;
                        break;
                    case "B":
                        pieceIcon = blackBishop;
                        break;
                    case "N":
                        pieceIcon = blackKnight;
                        break;
                    case "R":
                        pieceIcon = blackRook;
                        break;
                    case "Q":
                        pieceIcon = blackQueen;
                        break;
                    case "K":
                        pieceIcon = blackKing;
                        break;
                }
            }
            pieceIcon = new ImageIcon(resizeImage(pieceIcon));
            return pieceIcon;
        }

        private Image resizeImage(ImageIcon pieceIcon){
            Image image = pieceIcon.getImage();
            return image.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        }

        public Piece getPiece() {
            return piece;
        }

        // checks if there is a piece on the tile
        public boolean isOccupied(){
            return this.piece != null;
        }

        public int getPosition() {
            return position;
        }
    }

    /**
     * main method to run chess GUI
     */
    public static void main(String[] args) throws InterruptedException {
        Board board = new Board();
        // Custom FEN input
        //String FEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        board.init(FENUtilities.startFEN);

        ChessGUI chessGUI = new ChessGUI(board);
        chessGUI.initGUI();

        //*** AI vs AI ***//

//        AI player1 = new AI(true, board);
//        AI player2 = new AI(false, board);
//        boolean playerToMove;
//        TimeUnit.MILLISECONDS.sleep(1500);
//        while(board.getAllLegalMoves().size() != 0){
//            playerToMove = board.isWhiteTurn();
//            short move;
//            if(player1.getTurn() == playerToMove){
//                move = player1.getBestMove(5);
//                Move movement = new Move(board, move);
//                movement.makeMove();
//            }
//            else{
//                move = player2.getMove();
//                Move movement = new Move(board, move);
//                movement.makeMove();
//            }
//            chessGUI.update();
//            chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));
//            if(board.getBlackPieces().getCount() == 1 && board.getWhitePieces().getCount() == 1){
//                break;
//            }
//        }
//        if(GameStatus.checkGameEnded(board)){
//            String gameState = GameStatus.getHowGameEnded();
//            JOptionPane.showMessageDialog(chessGUI, gameState,
//                    "Game Manager", JOptionPane.INFORMATION_MESSAGE);
//        }

//                  //*** Player vs AI ***//

        AI computerPlayer = new AI(false, board);   // computer plays as black
        boolean playerPrompted = false;
        do {
            if (computerPlayer.getTurn() == board.isWhiteTurn()) {
                // computer makes move
                System.out.println("Engine is thinking...");
                short move = computerPlayer.getBestMove(6); // search to depth 6
                Move movement = new Move(board, move);
                movement.makeMove();
                chessGUI.update();
                chessGUI.showMovementTiles(MoveGenerator.getStart(move), MoveGenerator.getEnd(move));
                playerPrompted = false;
            } else {
                if(!playerPrompted){
                    System.out.println("Waiting for Player move...");
                    playerPrompted = true;
                }
                while (true) {
                    if (board.isWhiteTurn() != computerPlayer.getTurn()) {
                        TimeUnit.MILLISECONDS.sleep(1000);
                        break;
                    }
                }
            }
        } while ((board.getBlackPieces().getCount() != 1 || board.getWhitePieces().getCount() != 1) &&
                board.getAllLegalMoves().size() != 0);

        if(GameStatus.checkGameEnded(board)){
            String gameState = GameStatus.getHowGameEnded();
            JOptionPane.showMessageDialog(chessGUI, gameState,
                    "Game Manager", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
