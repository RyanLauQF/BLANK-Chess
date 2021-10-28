import javax.swing.*;

public class ChessGameMenu {
    public ChessGameMenu(){
        // creates the Jframe which stores the game menu
        JFrame frame = new JFrame("Chess Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
    }

    public static void main(String[] args) {
        ChessGameMenu menu = new ChessGameMenu();
    }
}
