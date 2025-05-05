import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;

public class CameraLiveFeed {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        VideoCapture capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.out.println("Error opening camera");
            System.exit(-1);
        }
        HighGui.namedWindow("Camera Feed");
        Mat frame = new Mat();
        while (true) {
            if (capture.read(frame)) {
                HighGui.imshow("Camera Feed", frame);
            }
            int key = HighGui.waitKey(30);
            if (key == 'q' || key == 27) { 
                break;
            }
        }
        capture.release();
        HighGui.destroyAllWindows();
        System.exit(0);
    }
}
