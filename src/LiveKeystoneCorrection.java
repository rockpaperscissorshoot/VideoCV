import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class LiveKeystoneCorrection {
    private static List<org.opencv.core.Point> points = new ArrayList<>();
    private static Mat perspectiveMatrix = null;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static int draggedPointIndex = -1;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        VideoCapture capture = new VideoCapture(0);

        if (!capture.isOpened()) {
            System.out.println("Error opening camera");
            return;
        }

        JFrame frame = new JFrame("Camera Feed");
        VideoPanel videoPanel = new VideoPanel();
        frame.setContentPane(videoPanel);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        JFrame correctedFrame = new JFrame("Corrected Image");
        CorrectedPanel correctedPanel = new CorrectedPanel();
        correctedFrame.setContentPane(correctedPanel);
        correctedFrame.setSize(800, 600);
        correctedFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        correctedFrame.setVisible(true);

        videoPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                double xScale = (double) frameWidth / videoPanel.getWidth();
                double yScale = (double) frameHeight / videoPanel.getHeight();
                double scaledX = e.getX() * xScale;
                double scaledY = e.getY() * yScale;

                for (int i = 0; i < points.size(); i++) {
                    org.opencv.core.Point p = points.get(i);
                    if (Math.hypot(p.x - scaledX, p.y - scaledY) < 20) {
                        draggedPointIndex = i;
                        return;
                    }
                }

                if (points.size() < 4) {
                    points.add(new org.opencv.core.Point(scaledX, scaledY));
                    System.out.println("Point added: " + scaledX + ", " + scaledY);
                    if (points.size() == 4) {
                        orderPoints();
                        updatePerspectiveMatrix();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedPointIndex = -1;
            }
        });

        videoPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedPointIndex != -1) {
                    double xScale = (double) frameWidth / videoPanel.getWidth();
                    double yScale = (double) frameHeight / videoPanel.getHeight();
                    double scaledX = e.getX() * xScale;
                    double scaledY = e.getY() * yScale;

                    points.set(draggedPointIndex, new org.opencv.core.Point(scaledX, scaledY));
                    updatePerspectiveMatrix();
                }
            }
        });

        Mat frameMat = new Mat();
        while (true) {
            if (!capture.read(frameMat) || frameMat.empty()) continue;

            frameWidth = frameMat.cols();
            frameHeight = frameMat.rows();

            Mat displayFrame = frameMat.clone();
            drawSelection(displayFrame);
            videoPanel.updateImage(matToBufferedImage(displayFrame));

            if (perspectiveMatrix != null) {
                Mat corrected = new Mat();
                Imgproc.warpPerspective(frameMat, corrected, perspectiveMatrix, new Size(frameWidth, frameHeight));
                correctedPanel.updateCorrectedImage(matToBufferedImage(corrected));
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void orderPoints() {
    }

    private static void updatePerspectiveMatrix() {
        Mat src = new MatOfPoint2f(points.toArray(new org.opencv.core.Point[0]));
        Mat dst = new MatOfPoint2f(
            new org.opencv.core.Point(0, 0),
            new org.opencv.core.Point(frameWidth - 1, 0),
            new org.opencv.core.Point(frameWidth - 1, frameHeight - 1),
            new org.opencv.core.Point(0, frameHeight - 1)
        );
        perspectiveMatrix = Imgproc.getPerspectiveTransform(src, dst);
    }

    private static void drawSelection(Mat frame) {
        for (org.opencv.core.Point p : points) {
            Imgproc.circle(frame, (p), 5, new Scalar(0, 0, 255), -1);
        }
        if (points.size() >= 2) {
            for (int i = 0; i < points.size(); i++) {
                org.opencv.core.Point p1 = points.get(i);
                org.opencv.core.Point p2 = points.get((i + 1) % points.size());
                Imgproc.line(frame, p1, p2, new Scalar(0, 255, 0), 2);
            }
        }
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else {
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
            mat = rgbMat;
        }
        byte[] data = new byte[mat.rows() * mat.cols() * mat.channels()];
        mat.get(0, 0, data);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    static class VideoPanel extends JPanel {
        private BufferedImage image;

        public void updateImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    static class CorrectedPanel extends JPanel {
        private BufferedImage correctedImage;

        public void updateCorrectedImage(BufferedImage img) {
            this.correctedImage = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (correctedImage != null) {
                g.drawImage(correctedImage, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }
}