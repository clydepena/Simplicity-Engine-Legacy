package util;

public class Debug {
    
    static boolean debug = true;

    public static String Print(String text) {
        if (debug) {
            System.out.println(text);
            return text;
        }
        return null;
    }


}
