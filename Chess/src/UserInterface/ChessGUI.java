import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class ChessGUI extends JPanel {
    // all white piece images
    public static final ImageIcon whitePawn = new ImageIcon("resources/piece-icons/whitePawn.png");
    public static final ImageIcon whiteKnight = new ImageIcon("resources/piece-icons/whiteKnight.png");
    public static final ImageIcon whiteBishop = new ImageIcon("resources/piece-icons/whiteBishop.png");
    public static final ImageIcon whiteRook = new ImageIcon("resources/piece-icons/whiteRook.png");
    public static final ImageIcon whiteQueen = new ImageIcon("resources/piece-icons/whiteQueen.png");
    public static final ImageIcon whiteKing = new ImageIcon("resources/piece-icons/whiteKing.png");

    // all black piece images
    public static final ImageIcon blackPawn = new ImageIcon("resources/piece-icons/blackPawn.png");
    public static final ImageIcon blackKnight = new ImageIcon("resources/piece-icons/blackKnight.png");
    public static final ImageIcon blackBishop = new ImageIcon("resources/piece-icons/blackBishop.png");
    public static final ImageIcon blackRook = new ImageIcon("resources/piece-icons/blackRook.png");
    public static final ImageIcon blackQueen = new ImageIcon("resources/piece-icons/blackQueen.png");
    public static final ImageIcon blackKing = new ImageIcon("resources/piece-icons/blackKing.png");

    // TILE COLOURS
    private static final Color DARK_SQUARE_COLOUR = new Color(182, 136, 96);
    private static final Color LIGHT_SQUARE_COLOUR = new Color(241, 218, 179);
    private static final Color SELECTED_SQUARE_COLOUR = new Color(187, 203, 61);
    private static final Color LIGHT_MOVE_SQUARE_COLOUR = new Color(226, 81, 76);
    private static final Color DARK_MOVE_SQUARE_COLOUR = new Color(215, 72, 64);

    private final Stack<Integer> highlightedTiles;

    private final Board board;    // reference to the current board in chess game
    private boolean hasPieceBeenSelected;   // checks if piece is selected
    private int pieceSelected;  // index of selected piece, if not selected, set to -1

    /**
     * Constructor creates a overall JPanel to represent the chess board user interface
     * @param board refers to the chess board represented by a Board object
     */
    public ChessGUI(Board board){
        this.board = board;
        this.hasPieceBeenSelected = false;
        this.pieceSelected = -1;
        this.highlightedTiles = new Stack<>();

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
    public void update(){
        Component[] components = this.getComponents();
        // iterate through all JPanels in ChessGUI overall panel
        for(int index = 0 ; index < components.length; index++){
            if(components[index] instanceof TilePanel){
                TilePanel currPanel = (TilePanel) components[index];
                Color backgroundColor = currPanel.getBackground();
                currPanel.removeAll();
                currPanel.setBackground(backgroundColor);
                if(board.getTile(index).isOccupied()){
                    currPanel.setPiece(board.getTile(index).getPiece());
                }
                else{
                    currPanel.setPiece(null);
                }
            }
        }
        this.revalidate();
        this.repaint();
        setHasPieceBeenSelected(false);
        setSelectedPiece(-1);
    }

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
                    if(currPanel.getBackground() == LIGHT_SQUARE_COLOUR){
                        currPanel.setBackground(LIGHT_MOVE_SQUARE_COLOUR);
                    }
                    else if(currPanel.getBackground() == DARK_SQUARE_COLOUR){
                        currPanel.setBackground(DARK_MOVE_SQUARE_COLOUR);
                    }
                    highlightedTiles.add(endPosition);
                }
            }
        }
    }

    public void select(int tilePosition){
        setSelectedPiece(tilePosition);
        setHasPieceBeenSelected(true);
        // Get individual JPanels from ChessGUI JPanel
        Component[] components = this.getComponents();
        // Highlight selected tile
        components[tilePosition].setBackground(SELECTED_SQUARE_COLOUR);
        // Show all legal tiles of the selected piece
        showLegalTiles();
    }

    public void deselect(){
        // reset highlighted tiles
        resetHighlightedTiles();

        // deselect the previous selected piece
        Component[] components = this.getComponents();
        components[getSelectedPiece()].setBackground(getTileOriginalColor(getSelectedPiece()));

        setSelectedPiece(-1);
        setHasPieceBeenSelected(false);
    }

    private void resetHighlightedTiles(){
        Component[] components = this.getComponents();
        while(!highlightedTiles.isEmpty()){
            int tileToBeReset = highlightedTiles.pop();
            components[tileToBeReset].setBackground(getTileOriginalColor(tileToBeReset));
        }
    }

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

    public boolean isPieceSelected(){
        return hasPieceBeenSelected;
    }

    public void setHasPieceBeenSelected(boolean isSelected){
        this.hasPieceBeenSelected = isSelected;
    }

    public int getSelectedPiece(){
        return pieceSelected;
    }

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

    public static class TilePanel extends JPanel {
        private final int position;
        private Piece piece;

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
                        int selectedPiece = gui.getSelectedPiece();
                        for(short moves : gui.board.getTile(selectedPiece).getPiece().getLegalMoves()){
                            int end = MoveGenerator.getEnd(moves);
                            if(end == getPosition()){
                                isLegal = true;
                                break;
                            }
                        }
                        if(!isLegal){   // if not a legal move, deselect the first move and return
                            gui.deselect();
                            return;
                        }
                        Move move = new Move(gui.board, gui.getSelectedPiece(), getPosition());
                        // if the move is legal, make the move on the board
                        move.makeMove();
                        // update board tiles
                        gui.deselect();
                        gui.update();
                        if(GameStatus.checkGameEnded(gui.board)){
                            String gameState = GameStatus.getHowGameEnded();
                            JOptionPane.showMessageDialog(gui, gameState,
                                    "Game Manager", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            });
        }

        public void setPiece(Piece piece) {
            this.piece = piece;
            JLabel pieceIcon = new JLabel(getPieceIcon(), SwingConstants.CENTER);
            this.add(pieceIcon);
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
                        pieceIcon = ChessGUI.whitePawn;
                        break;
                    case "B":
                        pieceIcon = ChessGUI.whiteBishop;
                        break;
                    case "N":
                        pieceIcon = ChessGUI.whiteKnight;
                        break;
                    case "R":
                        pieceIcon = ChessGUI.whiteRook;
                        break;
                    case "Q":
                        pieceIcon = ChessGUI.whiteQueen;
                        break;
                    case "K":
                        pieceIcon = ChessGUI.whiteKing;
                        break;
                }
            }
            else{
                switch (pieceToString) {
                    case "P":
                        pieceIcon = ChessGUI.blackPawn;
                        break;
                    case "B":
                        pieceIcon = ChessGUI.blackBishop;
                        break;
                    case "N":
                        pieceIcon = ChessGUI.blackKnight;
                        break;
                    case "R":
                        pieceIcon = ChessGUI.blackRook;
                        break;
                    case "Q":
                        pieceIcon = ChessGUI.blackQueen;
                        break;
                    case "K":
                        pieceIcon = ChessGUI.blackKing;
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
        String FEN = "8/P5k1/8/8/8/8/8/K7 w - - 0 1";
        board.init(FENUtilities.startFEN);

        ChessGUI chessGUI = new ChessGUI(board);
        chessGUI.initGUI();

        //*** random movement AI playing each other ***//

//        AI player1 = new AI(true, board);
//        AI player2 = new AI(false, board);
//        boolean playerToMove;
//        int start, end;
//        while(board.getAllLegalMoves().size() != 0){
//            playerToMove = board.isWhiteTurn();
//            short move;
//            if(player1.getTurn() == playerToMove){
//                move = player1.getMove();
//                start = MoveGenerator.getStart(move);
//                end = MoveGenerator.getEnd(move);
//                Move movement = new Move(board, start, end);
//                movement.makeMove();
//            }
//            else{
//                move = player2.getMove();
//                start = MoveGenerator.getStart(move);
//                end = MoveGenerator.getEnd(move);
//                Move movement = new Move(board, start, end);
//                movement.makeMove();
//            }
//            chessGUI.update();
//            if(board.getBlackPieces().getCount() == 1 && board.getWhitePieces().getCount() == 1){
//                break;
//            }
//        }
//        if(GameStatus.checkGameEnded(board)){
//            String gameState = GameStatus.getHowGameEnded();
//            JOptionPane.showMessageDialog(chessGUI, gameState,
//                    "Game Manager", JOptionPane.INFORMATION_MESSAGE);
//        }
    }
}
