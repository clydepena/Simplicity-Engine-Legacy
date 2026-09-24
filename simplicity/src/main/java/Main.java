/* TODO:
    -Not selecting objects in viewport while minimize
    -Object viewport selection is still offset
    -Text editor coding
    -JS scripting
*/

import simplicity.OldWindow;
import simplicity.TestApp;

public class Main {
    
    public static void main(String[] args) {
        boolean test = true;
        if (test) {
            TestApp app = new TestApp();
            app.run();
        } else {
            OldWindow window = OldWindow.get();
            window.run();
        }


        // DEBUG
        // WindowFont windowFont = new WindowFont();
        // windowFont.run();
    }
}
