import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class OpeningTrie {
    public static class Node {
        String move;
        int occurrence;
        boolean isLastMove;
        HashMap<String, Node> nextMoves;

        public Node(String move, boolean isLastMove){
            this.move = move;
            this.isLastMove = isLastMove;
            this.occurrence = 1;
            this.nextMoves = new HashMap<>();
        }

        public int getOccurrence(){
            return occurrence;
        }
    }

    private final Node rootNode;      // root node of the trie
    public Node moveTracker;   // keeps track of which move we are currently at in the opening phase
    private int size;

    /**
     * Default constructor
     */
    public OpeningTrie() {
        this.rootNode = new Node(null, false);  // create root node
        this.moveTracker = this.rootNode;
        this.size = 0;
    }

    /**
     * Constructor with colour parameter.
     * If isWhite, build white opening trie, else build black opening trie
     * @param isWhite refers to the colour of the opening book
     */
    public OpeningTrie(boolean isWhite) throws IOException {
        this.rootNode = new Node(null, false);  // create root node
        this.moveTracker = this.rootNode;
        this.size = 0;

        if(isWhite){
            this.buildTrie("whiteProcessedBook.txt");
        }
        else{
            this.buildTrie("blackProcessedBook.txt");
        }
    }

    public void buildTrie(String fileName) throws IOException {
        if(fileName == null){
            throw new IllegalArgumentException("Invalid file!");
        }

        // builds the opening tree based on PGN input
        InputStream inputStream = getClass().getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String fileLine;
        // build the trie by processing the file line by line for each move
        while((fileLine = reader.readLine()) != null){
            addMove(fileLine);  // adds the whole line to the trie
        }
    }

    public void addMove(String moveLine) {
        // start adding moves from the root node
        Node nodePtr = rootNode;
        // parse the move line
        String[] moves = moveLine.split(" ");
        for (int i = 0; i < moves.length; i++) {
            if (moves[i].equals("0-1") || moves[i].equals("1-0")) {
                // ignore game end notation
                continue;
            }
            if(nodePtr.nextMoves.containsKey(moves[i])){
                // go to the next move if it already exists
                nodePtr = nodePtr.nextMoves.get(moves[i]);
                nodePtr.occurrence++;   // increment the occurrence of the move
            }
            else{
                // if it does not exist, create a new node and add to the trie
                boolean isLast = (i == moves.length - 1);   // checks if it is the last move being made
                Node moveNode = new Node(moves[i], isLast);
                nodePtr.nextMoves.put(moves[i], moveNode);
                nodePtr = nodePtr.nextMoves.get(moves[i]);
                size++;
            }
        }
    }

    public void makeMove(String move) {
        if(hasNextMove(move)){
            moveTracker = moveTracker.nextMoves.get(move);
        }
        else{
            System.out.println("move no longer in book");
            moveTracker = null; // no longer using the opening book so set the moveTracker to null.
        }
    }

    public boolean hasNextMove(String move){
        return moveTracker.nextMoves.containsKey(move);
    }

    public boolean hasMoves(){
        return moveTracker != null;
    }

    // get next moves based on a occurrence probability in the opening PGN
    public String getNextMove() {
        ArrayList<String> allMoves = new ArrayList<>();
        for(String moves : moveTracker.nextMoves.keySet()){
            int occurrence = moveTracker.nextMoves.get(moves).getOccurrence();
            for(int i = 0; i < occurrence; i++){
                allMoves.add(moves);
            }
        }
        Random random = new Random();
        int randomIndex = random.nextInt(allMoves.size());
        return allMoves.get(randomIndex);
    }

    public Set<String> getSetOfBookMoves(){
        return moveTracker.nextMoves.keySet();
    }

    public int size(){
        return size;
    }

    /**
     * Unit test
     */
    public static void main(String[] args) throws IOException {
        OpeningTrie openingTrie = new OpeningTrie();
        //openingTrie.buildTrie("whiteProcessedBook.txt");
        openingTrie.buildTrie("blackProcessedBook.txt");
//        Node node = openingTrie.moveTracker;
//        for(String moves : node.nextMoves.keySet()){
//            System.out.println(moves + " " + openingTrie.moveTracker.nextMoves.get(moves).getOccurrence());
//        }
//        System.out.println(openingTrie.getNextMove());
//        System.out.println();

//        openingTrie.makeMove("d4");
//        node = openingTrie.moveTracker;
//        for(String moves : node.nextMoves.keySet()){
//            System.out.println(moves + " " + node.nextMoves.get(moves).getOccurrence());
//        }
//        System.out.println(openingTrie.getNextMove());
//        System.out.println();
//
//        openingTrie.makeMove("Nf6");
//        node = openingTrie.moveTracker;
//        for(String moves : node.nextMoves.keySet()){
//            System.out.println(moves + " " + node.nextMoves.get(moves).getOccurrence());
//        }
//        System.out.println(openingTrie.getNextMove());
        Node node = openingTrie.moveTracker;
        for(String moves : node.nextMoves.keySet()){
            System.out.println(moves + " " + openingTrie.moveTracker.nextMoves.get(moves).getOccurrence());
        }
        System.out.println();
        openingTrie.makeMove("d4");
        node = openingTrie.moveTracker;
        for(String moves : node.nextMoves.keySet()){
            System.out.println(moves + " " + node.nextMoves.get(moves).getOccurrence());
        }

        System.out.println();
        System.out.println("Next Move: " + openingTrie.getNextMove());
        System.out.println("Size of Trie: " + openingTrie.size());

        openingTrie.makeMove("hi");
        System.out.println(openingTrie.hasMoves());
    }
}
