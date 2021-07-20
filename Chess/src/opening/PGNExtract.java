import java.io.*;
import java.net.URL;

public class PGNExtract {
    public static void processPGN(String fileName) throws IOException {
        // processes the PGN files by creating a new PGN file containing only the moves inside
        URL path = PGNExtract.class.getResource(fileName);

        File file = new File(path.getFile());
        File tempFile = new File("tempFile.txt");

        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        int index = 0;
        String currentLine;
        while((currentLine = reader.readLine()) != null){
            if(currentLine.equals("") || currentLine.charAt(0) == '[' || currentLine.charAt(0) != '1'){
                // ignore all empty lines and lines which start with '['.
                // checks if the line starts with 1. which represents move 1
                // information in brackets are not required
                continue;
            }
            // each line contains the moves for an entire game
            // process up to move 8. (everything after move 9 can be ignored)
            StringBuilder processedLines = new StringBuilder();
            for(int i = 0; i < currentLine.length(); i++){
                char ch = currentLine.charAt(i);
                if(Character.isDigit(ch)){
                    if(i + 1 < currentLine.length()){
                        int checkNext = i + 1;
                        // once reached the 9th move break and stop processing.
                        if(Character.getNumericValue(ch) == 9 && currentLine.charAt(checkNext) == '.'){
                            break;
                        }
                        else{
                            // filter out the numbers "1. 2. 3. ..." from each line to prepare them to be processed into the opening trie
                            if (currentLine.charAt(checkNext) == '.') {
                                i += 2;
                                continue;
                            }
                        }

                    }
                }
                processedLines.append(ch);
            }
            String processed = processedLines.toString();
            writer.write(processed + "\n");
            index++;
        }
        writer.flush();
        writer.close();
        reader.close();
        System.out.println("Processed " + index + " lines");
    }

    public static void main(String[] args) throws IOException {
        PGNExtract.processPGN("black2600.pgn");
    }
}
