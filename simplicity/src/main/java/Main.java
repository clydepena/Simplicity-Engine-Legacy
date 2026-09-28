/* TODO:
    -Not selecting objects in viewport while minimize
    -Object viewport selection is still offset
    -Text editor coding
    -JS scripting
*/

// import simplicity.OldWindow;
import logger.ConsoleCapture;
import logger.Logger;
import simplicity.Tasks;
import simplicity.TestApp;

public class Main {
    
    public static void main(String[] args) {
        // first: this thread is the main thread (Logger needs to know, for prints from workers)
        Tasks.init();
        // so everything printed from here on also becomes a log entry
        ConsoleCapture.install(Logger::info, Logger::error);

        boolean test = true;
        if (test) {
            TestApp app = new TestApp();
            app.run();
        } else {
            // OldWindow window = OldWindow.get();
            // window.run();
        }


        // DEBUG
        // WindowFont windowFont = new WindowFont();
        // windowFont.run();
    }
}
