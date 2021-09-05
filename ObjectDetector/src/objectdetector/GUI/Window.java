package objectdetector.GUI;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javax.swing.JFrame;
import static javax.swing.text.StyleConstants.Background;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class Window extends javax.swing.JFrame {

    private ImagePanel imagePanel, childPanel;
    private ColorPanel colorPanel;
    private VideoCapture capture;
    private Mat webCamImage;
    private Robot robot;
    private boolean detecting;
    private Color detectedColor;
    private Scalar minScalar, maxScalar;
    private Scalar scalar;
    private final int DEFAULT_THRESHOLD = 20;
    private final int DEFAULT_HEIGHT = 100;
    private final int DEFAULT_WIDTH = 100;
    private final int MAX_HEIGHT = 300;
    private final int MAX_WIDTH = 300;
    private final int MIN_HEIGHT = 10;
    private final int MIN_WIDTH = 10;
    private final int MIN_DELAY = 50;
    private final int DEFAULT_DELAY = 200;
    private final int MAX_DELAY = 2000;
    private final int MIN_JUMPTHRESHOLD = 100;
    private final int DEF_JUMPTHRESHOLD = 200;
    private final int DEF_MOVETHRESHOLD = 200;
    private final int MIN_MOVETHRESHOLD = 100;

    //motion variables
    private int mouseX;
    private int mouseY;
    private int delay;
    private int jumpThreshold, moveThreshold;
    private int H,S,V;
    private enum ImageType {

        RGB, GRAY_SCALE, HSV, THRESHOLDED
    };
    private ImageType imageType;
    private int width, height;
    private int colorThreshold;

    public Window() {
        initComponents();
        init();
        addWindowListener();
//        this.redSlider.setExtent(100);
        this.HSlider.setMaximum(255);
        this.SSlider.setMaximum(255);
        this.VSlider.setMaximum(255);
    }

    private void addWindowListener() {
        WindowAdapter windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                capture.release();
                webCamImage.release();
                dispose();
                System.exit(0);
            }
        };
        addWindowListener(windowAdapter);

    }

    private void loadDefaultMouseSettings() {
        delay = Const.DEFAULT_DELAY;
        jumpThreshold = Const.DEF_JUMPTHRESHOLD;
        moveThreshold = Const.DEF_MOVETHRESHOLD;
        delayTextField.setText("" + delay);
        jumpThresholdField.setText("" + jumpThreshold);
        moveThresholdField.setText("" + moveThreshold);
    }

    private void setThresholdValue() {
        try {
            colorThreshold = Integer.parseInt(thresholdField.getText());
            if (colorThreshold < 1 || colorThreshold > 255) {
                colorThreshold = Const.DEFAULT_COLOR_THRESHOLD;
                thresholdField.setText("" + this.DEFAULT_THRESHOLD);

            }
        } catch (Exception e) {
            colorThreshold = DEFAULT_THRESHOLD;
        }
    }

    private void getAndSetDetectingAreaDimension() {
        try {
            width = Integer.parseInt(this.objectWidth.getText());
            height = Integer.parseInt(this.objectHeight.getText());
            if (width < MIN_WIDTH || height < MIN_HEIGHT || this.width > this.MAX_WIDTH || this.height > this.MAX_HEIGHT) {
                setDefaultDimensions();
            }
        } catch (Exception e) {
            setDefaultDimensions();
        }
    }

    private void setDefaultDimensions() {
        width = DEFAULT_WIDTH;
        height = DEFAULT_HEIGHT;
        this.objectWidth.setText("" + DEFAULT_WIDTH);
        this.objectHeight.setText("" + DEFAULT_HEIGHT);
    }

    private void init() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
        }
        detecting = true;
        imageType = ImageType.RGB;
        imagePanel = new ImagePanel();
        childPanel = new ImagePanel();
        colorPanel = new ColorPanel();
        mainPanel.add(imagePanel);
        childMainPanel.add(colorPanel);
        parentPanel.add(childPanel);
        colorPanel.setBackground(Color.BLACK);
        colorPanel.setSize(childMainPanel.getWidth(), childMainPanel.getHeight());
        childMainPanel.setBackground(Color.BLUE);
        System.loadLibrary("opencv_java2410");
        //-Djava.library.path="C:\opencv\build\java\x64"
        System.out.print(Core.NATIVE_LIBRARY_NAME);
        capture = new VideoCapture(0);
        webCamImage = new Mat();
        capture.read(webCamImage);
        setDefaultDimensions();
        loadDefaultMouseSettings();
        imagePanel.setSize(webCamImage.width(), webCamImage.height());
        childPanel.setSize(width, height);
        setTitle("Object Detector " + webCamImage.width() + " x " + webCamImage.height());
    }

    private double threshold(double f, int t) {
        if (f + t < 0) {
            f = 0;
        } else if (f + t > 255) {
            f = 255;
        } else {
            f += t;
        }

        return f;
    }

    private double average(double[] d) {
        double sum = 0;
        for (int i = 0; i < d.length; ++i) {
            sum += d[i];
        }
        return sum / d.length;
    }

    private void pressArrowKey(int pX, int pY, int cX, int cY) {
        if (pY - cY > jumpThreshold) {
            robot.keyPress(KeyEvent.VK_UP);
            movement.setText("Up");
        } else if (pY - cY < -jumpThreshold) {
            robot.keyPress(KeyEvent.VK_DOWN);
            movement.setText("Down");

        }
        if (pX - cX > moveThreshold) {
            robot.keyPress(KeyEvent.VK_LEFT);
            movement.setText("Left");

        } else if (pX - cX < -moveThreshold) {
            robot.keyPress(KeyEvent.VK_RIGHT);
            movement.setText("Right");
        }

    }

    public void readCam() {
        mouseX = 0;
        mouseY = 0;
        new Thread() {
            int pY, pX, cX, cY;

            public void run() {
                while (true) {
                    try {
                        pY = mouseY;
                        pX = mouseX;
                        sleep(delay);
                        cX = mouseX;
                        cY = mouseY;
                        if (!detecting) {
                            pressArrowKey(pX, pY, cX, cY);
                        }

                    } catch (InterruptedException ie) {
                    }
                }
            }
        }.start();
        if (capture.isOpened()) {
            Mat thresholded = new Mat();
            Mat circles = new Mat();
            while (true) {
                if (capture.isOpened()) {
                    capture.read(webCamImage);
                    Core.flip(webCamImage, webCamImage, 1);
                    if (!webCamImage.empty()) {
                        if (detecting) {
                            Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2RGB);

                            Mat submat = webCamImage.submat(0, height - 1, 0, width - 1);

                            scalar = Core.sumElems(submat);
                            double elements = width * height;
                            scalar.val[0] /= elements;
                            scalar.val[1] /= elements;
                            scalar.val[2] /= elements;
                            detectingColor.setText("[ R:" + (int) scalar.val[0] + " G:" + (int) scalar.val[1] + " B:" + (int) scalar.val[2] + " ]");
                            detectedColor = new Color((int) scalar.val[0], (int) scalar.val[1], (int) scalar.val[2]);
                            colorPanel.setBackground(detectedColor);
                            colorPanel.repaint();
                            switch (imageType) {
                                case GRAY_SCALE:
                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2GRAY);
                                    break;
                                case HSV:
                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2HSV);
                                    break;
                            }
                            childPanel.setimagewithMat(submat);
                            imagePanel.setimagewithMat(webCamImage);
                            repaint();

                        } else {
                            Core.inRange(webCamImage, minScalar, maxScalar, thresholded);
                            Imgproc.GaussianBlur(thresholded, thresholded, new Size(5, 5), 0, 0);
                            Imgproc.HoughCircles(thresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, thresholded.height() / 4, 500, 60, 0, 0);

                            int rows = circles.rows();
                            int elemSize = (int) circles.elemSize(); // Returns 12 (3 * 4bytes in a float)  
                            float[] data = new float[rows * elemSize / 4];
                            if (data.length > 0) {
                                circles.get(0, 0, data); // Points to the first element and reads the whole thing  
                                for (int i = 0; i < data.length; i = i + 3) {
                                    Point center = new Point(data[i], data[i + 1]);
                                    Size size = new Size((double) data[i + 2], (double) data[i + 2]);
                                    Core.ellipse(webCamImage, center, size, 0, 0, 360, new Scalar(0, 0, 255), 4);
                                    mouseY = (int) (center.y + mainPanel.getY());
                                    mouseX = (int) (center.x + mainPanel.getX());
                                            robot.mouseMove(mouseX, mouseY);
                                    position.setText("X " + mouseX + " Y " + mouseY);
                                }
                            }
                            Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2RGB);
                            switch (imageType) {
                                case GRAY_SCALE:
                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2GRAY);
                                    break;
                                case HSV:
                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2HSV);
                                    break;
                                case THRESHOLDED:
                                    Core.inRange(thresholded, minScalar, maxScalar, webCamImage);
                            }

                            imagePanel.setimagewithMat(webCamImage);
                            repaint();
                        }

                    }
                }
            }
        }

    }
 public void readCam1() {
        if (capture.isOpened()) {
            Mat thresholded = new Mat();
            Mat circles = new Mat();
            
            while (true) {
                if (capture.isOpened()) {
                    capture.read(webCamImage);
                    Core.flip(webCamImage, webCamImage, 1);
                    if (!webCamImage.empty()) {
{
                                        Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_RGB2HSV);
                            Imgproc.GaussianBlur(webCamImage, thresholded, new Size(5, 5), 0, 0);
                         Core.subtract(webCamImage, thresholded, webCamImage);
                                        
   //                           Core.inRange(webCamImage, new Scalar(threshold(H,-10),threshold(S,-10),threshold(V,-10)), new Scalar(threshold(H,10),threshold(S,10),threshold(V,10)),webCamImage);
//                                     
                              
//
//                            Core.inRange(webCamImage, minScalar, maxScalar, thresholded);
//                            Imgproc.GaussianBlur(thresholded, thresholded, new Size(5, 5), 0, 0);
//                            Imgproc.HoughCircles(thresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, thresholded.height() / 4, 500, 60, 0, 0);
//
//                            int rows = circles.rows();
//                            int elemSize = (int) circles.elemSize(); // Returns 12 (3 * 4bytes in a float)  
//                            float[] data = new float[rows * elemSize / 4];
//                            if (data.length > 0) {
//                                circles.get(0, 0, data); // Points to the first element and reads the whole thing  
//                                for (int i = 0; i < data.length; i = i + 3) {
//                                    Point center = new Point(data[i], data[i + 1]);
//                                    Size size = new Size((double) data[i + 2], (double) data[i + 2]);
//                                    Core.ellipse(webCamImage, center, size, 0, 0, 360, new Scalar(0, 0, 255), 4);
//                                    mouseY = (int) (center.y + mainPanel.getY());
//                                    mouseX = (int) (center.x + mainPanel.getX());
//                                            robot.mouseMove(mouseX, mouseY);
//                                    position.setText("X " + mouseX + " Y " + mouseY);
//                                }
//                            }
//                            Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2RGB);
//                            switch (imageType) {
//                                case GRAY_SCALE:
//                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2GRAY);
//                                    break;
//                                case HSV:
//                                    Imgproc.cvtColor(webCamImage, webCamImage, Imgproc.COLOR_BGR2HSV);
//                                    break;
//                                case THRESHOLDED:
//                                    Core.inRange(thresholded, minScalar, maxScalar, webCamImage);
//
//                            }
//
                            imagePanel.setimagewithMat(webCamImage);
                            repaint();
                        }

                    }
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        objectWidth = new javax.swing.JTextField();
        objectHeight = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        label = new javax.swing.JLabel();
        childMainPanel = new javax.swing.JPanel();
        selectColor = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        resetColor = new javax.swing.JButton();
        detectingColor = new javax.swing.JLabel();
        thresholdField = new javax.swing.JTextField();
        imageTypeList = new javax.swing.JComboBox();
        parentPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jumpThresholdField = new javax.swing.JTextField();
        moveThresholdField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        delayTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        position = new javax.swing.JLabel();
        configure = new javax.swing.JButton();
        movement = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        HSlider = new javax.swing.JSlider();
        SSlider = new javax.swing.JSlider();
        VSlider = new javax.swing.JSlider();
        hLabel = new javax.swing.JLabel();
        sLabel = new javax.swing.JLabel();
        vLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 644, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 456, Short.MAX_VALUE)
        );

        objectWidth.setText("100");
        objectWidth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                objectWidthActionPerformed(evt);
            }
        });

        objectHeight.setText("100");
        objectHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                objectHeightActionPerformed(evt);
            }
        });

        jLabel1.setText("X");

        label.setText("Threshold");

        javax.swing.GroupLayout childMainPanelLayout = new javax.swing.GroupLayout(childMainPanel);
        childMainPanel.setLayout(childMainPanelLayout);
        childMainPanelLayout.setHorizontalGroup(
            childMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 69, Short.MAX_VALUE)
        );
        childMainPanelLayout.setVerticalGroup(
            childMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        selectColor.setText("Capture Color");
        selectColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectColorActionPerformed(evt);
            }
        });

        jLabel2.setText("Dimension");

        resetColor.setText("Reset");
        resetColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetColorActionPerformed(evt);
            }
        });

        detectingColor.setText("[ R: G: B: ]");

        thresholdField.setText("20");

        imageTypeList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "RGB", "GRAY SCALE", "HSV" }));
        imageTypeList.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                imageTypeListItemStateChanged(evt);
            }
        });
        imageTypeList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageTypeListActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(objectWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(objectHeight, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(label, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(thresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resetColor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(detectingColor, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(139, 139, 139)
                .addComponent(childMainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectColor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageTypeList, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(51, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(objectWidth)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(objectHeight)
            .addComponent(childMainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(thresholdField)
            .addComponent(resetColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(imageTypeList, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectColor, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(detectingColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout parentPanelLayout = new javax.swing.GroupLayout(parentPanel);
        parentPanel.setLayout(parentPanelLayout);
        parentPanelLayout.setHorizontalGroup(
            parentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        parentPanelLayout.setVerticalGroup(
            parentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 151, Short.MAX_VALUE)
        );

        jumpThresholdField.setText("150");

        moveThresholdField.setText("150");
        moveThresholdField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveThresholdFieldActionPerformed(evt);
            }
        });

        jLabel5.setText("Move Threshold");

        jLabel6.setText("Delay(ms)");

        delayTextField.setText("200");
        delayTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delayTextFieldActionPerformed(evt);
            }
        });

        jLabel4.setText("Jump Threshold");

        configure.setText("Configure");
        configure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jumpThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(moveThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(position, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(movement, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(configure)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jumpThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(moveThresholdField, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(position, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(movement, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(configure, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        HSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                HSliderStateChanged(evt);
            }
        });

        SSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                SSliderStateChanged(evt);
            }
        });

        VSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                VSliderStateChanged(evt);
            }
        });

        hLabel.setText("H:");

        sLabel.setText("S:");

        vLabel.setText("V:");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(VSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(vLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(HSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(hLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(hLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(HSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(vLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(VSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 50, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(parentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(102, 102, 102)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(parentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(141, 141, 141))))
        );

        jLabel3.setText("After setting the Dimensions and color threshold level, click \"Reset\" and move the Object to be detected to the top-left corner of WebCam video and click '' Capture Color''. ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addGap(38, 38, 38)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void selectColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectColorActionPerformed
        // TODO add your handling code here:

        if (this.detectedColor != null) {

            double minData[] = new double[3];
            double maxData[] = new double[3];

            minData[0] = scalar.val[0];
            minData[1] = scalar.val[1];
            minData[2] = scalar.val[2];

            maxData[0] = scalar.val[0];
            maxData[1] = scalar.val[1];
            maxData[2] = scalar.val[2];

            for (int i = 0; i < 3; ++i) {
                minData[i] = threshold(minData[i], -colorThreshold);
                maxData[i] = threshold(maxData[i], +colorThreshold);
            }

            this.minScalar = new Scalar(minData[2], minData[1], minData[0]);
            this.maxScalar = new Scalar(maxData[2], maxData[1], maxData[0]);
            detecting = false;
            imageTypeList.removeItem("THRESHOLDED");
            imageTypeList.addItem("THRESHOLDED");
        }
    }//GEN-LAST:event_selectColorActionPerformed

    private void resetColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetColorActionPerformed
        // TODO add your handling code here:
        detecting = true;
        getAndSetDetectingAreaDimension();
        setThresholdValue();
        childPanel.setSize(width, height);
        imageTypeList.removeItem("THRESHOLDED");

    }//GEN-LAST:event_resetColorActionPerformed

    private void objectWidthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_objectWidthActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_objectWidthActionPerformed

    private void objectHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_objectHeightActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_objectHeightActionPerformed

    private void imageTypeListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageTypeListActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_imageTypeListActionPerformed

    private void imageTypeListItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_imageTypeListItemStateChanged
        // TODO add your handling code here:
        String imgType = evt.getItem().toString();
        if (imgType.equals("RGB")) {
            this.imageType = ImageType.RGB;
        } else if (imgType.equals("GRAY SCALE")) {
            this.imageType = ImageType.GRAY_SCALE;
        } else if (imgType.equals("HSV")) {
            this.imageType = ImageType.HSV;
        } else if (imgType.equals("THRESHOLDED")) {
            this.imageType = ImageType.THRESHOLDED;
        }

    }//GEN-LAST:event_imageTypeListItemStateChanged

    private void delayTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delayTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_delayTextFieldActionPerformed

    private void moveThresholdFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveThresholdFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_moveThresholdFieldActionPerformed

    private void configureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureActionPerformed
        // TODO add your handling code here:
        try {
            delay = Integer.parseInt(delayTextField.getText());
            if (delay < Const.MIN_DELAY) {
                delay = Const.MIN_DELAY;
                delayTextField.setText("" + delay);
            } else if (delay > Const.MAX_DELAY) {
                delay = Const.MAX_DELAY;
                delayTextField.setText("" + delay);
            }
        } catch (Exception e) {
            delay = Const.DEFAULT_DELAY;
            delayTextField.setText("" + delay);
        }
        try {
            jumpThreshold = Integer.parseInt(jumpThresholdField.getText());
            if (jumpThreshold < Const.MIN_JUMPTHRESHOLD) {
                jumpThreshold = Const.MIN_JUMPTHRESHOLD;
                jumpThresholdField.setText("" + jumpThreshold);
            }

        } catch (Exception e) {
            jumpThreshold = Const.DEF_JUMPTHRESHOLD;
            jumpThresholdField.setText("" + jumpThreshold);
        }
        try {
            moveThreshold = Integer.parseInt(moveThresholdField.getText());
            if (moveThreshold < Const.MIN_MOVETHRESHOLD) {
                moveThreshold = Const.MIN_MOVETHRESHOLD;
                moveThresholdField.setText("" + moveThreshold);
            }

        } catch (Exception e) {
            moveThreshold = Const.DEF_MOVETHRESHOLD;
            moveThresholdField.setText("" + moveThreshold);
        }

    }//GEN-LAST:event_configureActionPerformed

    private void HSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_HSliderStateChanged
        // TODO add your handling code here:
        H=HSlider.getModel().getValue();
this.hLabel.setText("H:"+H);

    }//GEN-LAST:event_HSliderStateChanged

    private void SSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SSliderStateChanged
        // TODO add your handling code here:
     //   this
        S=SSlider.getModel().getValue();
        this.sLabel.setText("S:"+S);
    }//GEN-LAST:event_SSliderStateChanged

    private void VSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_VSliderStateChanged
        // TODO add your handling code here:
        V=VSlider.getModel().getValue();
        this.vLabel.setText("V:"+V);
    }//GEN-LAST:event_VSliderStateChanged

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider HSlider;
    private javax.swing.JSlider SSlider;
    private javax.swing.JSlider VSlider;
    private javax.swing.JPanel childMainPanel;
    private javax.swing.JButton configure;
    private javax.swing.JTextField delayTextField;
    private javax.swing.JLabel detectingColor;
    private javax.swing.JLabel hLabel;
    private javax.swing.JComboBox imageTypeList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField jumpThresholdField;
    private javax.swing.JLabel label;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextField moveThresholdField;
    private javax.swing.JLabel movement;
    private javax.swing.JTextField objectHeight;
    private javax.swing.JTextField objectWidth;
    private javax.swing.JPanel parentPanel;
    private javax.swing.JLabel position;
    private javax.swing.JButton resetColor;
    private javax.swing.JLabel sLabel;
    private javax.swing.JButton selectColor;
    private javax.swing.JTextField thresholdField;
    private javax.swing.JLabel vLabel;
    // End of variables declaration//GEN-END:variables
}
