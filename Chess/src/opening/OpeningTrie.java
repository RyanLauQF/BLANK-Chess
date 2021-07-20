import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class OpeningTrie {
    private static class Node {
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
    private Node moveTracker;   // keeps track of which move we are currently at in the opening phase
    private int size;

    public OpeningTrie() {
        this.rootNode = new Node(null, false);  // create root node
        this.moveTracker = this.rootNode;
        this.size = 0;
    }

    public void buildTrie(String fileName) throws IOException {
        if(fileName == null){
            throw new IllegalArgumentException("Invalid file!");
        }
        // gets the path to the file
        URL path = getClass().getResource(fileName);
        // builds the opening tree based on PGN input
        File processedPGN = new File(path.getFile());
        BufferedReader reader = new BufferedReader(new FileReader(processedPGN));
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
        }
    }

    public boolean hasNextMove(String move){
        return moveTracker.nextMoves.containsKey(move);
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

    public int treeSize(){
        return size;
    }

    /**
     * Unit test
     */
    public static void main(String[] args) throws IOException {
        OpeningTrie openingTrie = new OpeningTrie();
        openingTrie.buildTrie("tempFile.txt");
        Node node = openingTrie.moveTracker;
        for(String moves : node.nextMoves.keySet()){
            System.out.println(moves + " " + openingTrie.moveTracker.nextMoves.get(moves).getOccurrence());
        }
        System.out.println(openingTrie.getNextMove());
        System.out.println();

        openingTrie.makeMove("d4");
        node = openingTrie.moveTracker;
        for(String moves : node.nextMoves.keySet()){
            System.out.println(moves + " " + node.nextMoves.get(moves).getOccurrence());
        }
        System.out.println(openingTrie.getNextMove());
        System.out.println();

        openingTrie.makeMove("Nf6");
        node = openingTrie.moveTracker;
        for(String moves : node.nextMoves.keySet()){
            System.out.println(moves + " " + node.nextMoves.get(moves).getOccurrence());
        }
        System.out.println(openingTrie.getNextMove());

        System.out.println(openingTrie.treeSize());
    }
}
