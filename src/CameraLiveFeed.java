import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;

public class CameraLiveFeed {
    static {
        // Load the OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        // Create VideoCapture object to access the camera
        VideoCapture capture = new VideoCapture(0);

        // Check if camera opened successfully
        if (!capture.isOpened()) {
            System.out.println("Error opening camera");
            System.exit(-1);
        }

        // Create window to display camera feed
        HighGui.namedWindow("Camera Feed");

        // Matrix to store each frame
        Mat frame = new Mat();

        // Read frames continuously
        while (true) {
            if (capture.read(frame)) {
                // Display the frame
                HighGui.imshow("Camera Feed", frame);
            }

            // Break the loop if 'q' or ESC is pressed
            int key = HighGui.waitKey(30);
            if (key == 'q' || key == 27) { // 27 is ASCII for ESC
                break;
            }
        }

        // Release resources
        capture.release();
        HighGui.destroyAllWindows();
        System.exit(0);
    }
}
