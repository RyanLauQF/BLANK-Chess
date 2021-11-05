import java.io.IOException;

public class Player extends EngineMain{
    protected final boolean isHuman;
    protected final boolean isWhite;

    Player(boolean isWhite, boolean isHuman, Board board) throws IOException {
        super(board, true);
        this.isHuman = isHuman;
        this.isWhite = isWhite;
    }

    public Board getBoard(){
        return board;
    }

    public boolean isWhite(){
        return isWhite;
    }

    public boolean isHuman(){
        return isHuman;
    }
}
