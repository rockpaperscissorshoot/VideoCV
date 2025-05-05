import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.*;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class TrapezoidRectifier {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private static final int OUTPUT_WIDTH = 400;
    private static final int OUTPUT_HEIGHT = 300;

    public static final ArrayList<Point> selectedPoints = new ArrayList<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Trapezoid Selector");
        VideoCapture capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.out.println("Cannot open camera");
            return;
        }

        VideoPanel mainPanel = new VideoPanel(capture, true);
        frame.setContentPane(mainPanel);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(() -> {
            Mat frameMat = new Mat();
            while (true) {
                capture.read(frameMat);
                if (!frameMat.empty()) {
                    mainPanel.setFrame(frameMat.clone());
                }
            }
        }).start();
    }

    static class VideoPanel extends JPanel {
        private BufferedImage image;
        private Mat currentFrame;
        private final VideoCapture capture;
        private final boolean isMain;

        public VideoPanel(VideoCapture capture, boolean isMain) {
            this.capture = capture;
            this.isMain = isMain;

            if (isMain) {
                addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (selectedPoints.size() < 4) {
                            selectedPoints.add(new Point(e.getX(), e.getY()));
                        }
                        if (selectedPoints.size() == 4) {
                            JFrame rectifiedFrame = new JFrame("Rectified Live View");
                            VideoPanel rectifiedPanel = new VideoPanel(capture, false);
                            rectifiedFrame.setContentPane(rectifiedPanel);
                            rectifiedFrame.setSize(OUTPUT_WIDTH + 20, OUTPUT_HEIGHT + 40);
                            rectifiedFrame.setVisible(true);
                        }
                    }
                });
            }

            Timer timer = new Timer(33, e -> repaint()); 
            timer.start();
        }

        public void setFrame(Mat frame) {
            this.currentFrame = frame;
            this.image = matToBufferedImage(frame);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentFrame == null) return;

            BufferedImage displayImage;
            if (!isMain && selectedPoints.size() == 4) {
                displayImage = matToBufferedImage(getWarpedPerspective(currentFrame));
            } else {
                displayImage = matToBufferedImage(currentFrame);
            }

            g.drawImage(displayImage, 0, 0, getWidth(), getHeight(), null);

            if (isMain) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.RED);
                for (int i = 0; i < selectedPoints.size(); i++) {
                    Point p = selectedPoints.get(i);
                    g2.fillOval((int) p.x - 4, (int) p.y - 4, 8, 8);
                    if (i > 0) {
                        Point prev = selectedPoints.get(i - 1);
                        g2.drawLine((int) prev.x, (int) prev.y, (int) p.x, (int) p.y);
                    }
                }
                if (selectedPoints.size() == 4) {
                    Point first = selectedPoints.get(0);
                    Point last = selectedPoints.get(3);
                    g2.drawLine((int) first.x, (int) first.y, (int) last.x, (int) last.y);
                }
            }
        }

        private Mat getWarpedPerspective(Mat src) {
            int w = getWidth();
            int h = getHeight();

            // Adjust for scaling
            double xScale = (double) src.cols() / w;
            double yScale = (double) src.rows() / h;

            Point[] scaledPoints = selectedPoints.stream()
                .map(p -> new Point(p.x * xScale, p.y * yScale))
                .toArray(Point[]::new);

            MatOfPoint2f srcPoints = new MatOfPoint2f(
                scaledPoints[0], scaledPoints[1], scaledPoints[2], scaledPoints[3]
            );

            MatOfPoint2f dstPoints = new MatOfPoint2f(
                new Point(0, 0),
                new Point(OUTPUT_WIDTH, 0),
                new Point(OUTPUT_WIDTH, OUTPUT_HEIGHT),
                new Point(0, OUTPUT_HEIGHT)
            );

            Mat transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
            Mat output = new Mat();
            Imgproc.warpPerspective(src, output, transform, new Size(OUTPUT_WIDTH, OUTPUT_HEIGHT));
            return output;
        }

        private BufferedImage matToBufferedImage(Mat mat) {
            int type = BufferedImage.TYPE_3BYTE_BGR;
            if (mat.channels() == 1) type = BufferedImage.TYPE_BYTE_GRAY;
            byte[] data = new byte[mat.rows() * mat.cols() * mat.channels()];
            mat.get(0, 0, data);
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
            return image;
        }
    }
}
