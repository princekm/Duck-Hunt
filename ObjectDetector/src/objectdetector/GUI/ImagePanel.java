package objectdetector.GUI;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.opencv.core.Mat;

public class ImagePanel extends JPanel {

    private BufferedImage image;

    public ImagePanel() {
        super();
    }

    private BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage newImage) {
        image = newImage;
    }

    public void setimagewithMat(Mat matrix) {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int)matrix.elemSize();
        
        byte[] data = new byte[cols * rows * elemSize];
        int type;
        matrix.get(0, 0, data);
        switch (matrix.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb  
//                byte b;
//                for (int i = 0; i < data.length; i = i + 3) {
//                    b = data[i];
//                    data[i] = data[i + 2];
//                    data[i + 2] = b;
//                }
                break;
            default:
                type = 0;
        }
        image = new BufferedImage(cols, rows, type);
        image.getRaster().setDataElements(0, 0, cols, rows, data);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
        }
    }
}
