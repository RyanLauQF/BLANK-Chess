import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageCutter {
    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File("src/allChessPieces.png"));

        int width = image.getWidth();
        int height = image.getHeight();

        int pieceWidth = width/6;
        int pieceHeight = height/2;

        int x, y = 0;

        int counter = 0;

        for(int i = 0; i < 2; i++){
            x = 0;
            for(int j = 0; j < 6; j++) {
                try {
                    System.out.println("creating piece: "+i+" "+j + " " + x + " " + y);
                    BufferedImage subImage = image.getSubimage(x, y, pieceWidth, pieceHeight);
                    ImageIO.write(subImage, "png", new File("Piece" + counter + ".png"));
                    counter++;
                    x += pieceWidth;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            y += pieceHeight;
        }
    }
}
