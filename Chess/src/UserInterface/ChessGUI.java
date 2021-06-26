import javax.swing.*;
import java.awt.*;

public class ChessGUI extends JFrame {

    private final TilePanel[][] boardTiles = new TilePanel[8][8];
    private Board board;    // reference to the current board in chess game

    public ChessGUI(Board board){
        this.board = board;
        Dimension dimension = new Dimension(64, 64);
        setLayout(new GridLayout(8, 8));
        // create Tile panels
        // initialise colours of the tiles on the board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                TilePanel b = new TilePanel((i * 8) + j);
                b.setPreferredSize(dimension);
                b.setMinimumSize(dimension);
                if ((i + j + 1) % 2 == 0) {
                    b.setBackground(new Color(241, 218, 179)); // light squares
                } else {
                    b.setBackground(new Color(182, 136, 96)); // dark squares
                }
                add(b);
                boardTiles[i][j] = b;
            }
        }

        // initialise pieces onto the board
    }

    public static void initGUI(Board b){
        ChessGUI chessPanel = new ChessGUI(b);
        JFrame frame = new JFrame("Chess Interface");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(chessPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    private class TilePanel extends JPanel{
        private final int position;

        TilePanel(final int position){
            super(new GridLayout());
            this.position = position;
        }




    }
    public static void main(String[] args) {
        Board b = new Board();
        String FEN = "8/8/8/6k1/1q6/8/8/K6R w - - 0 1";
        b.init(FEN);
        ChessGUI.initGUI(b);
    }
}
