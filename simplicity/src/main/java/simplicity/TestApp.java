package simplicity;

public class TestApp extends Application{

    public TestApp() {
        super("Test");

        pushLayer(new TestLayer());
    }

    @Override
    protected void onClose() {
        System.out.println(title + " closed");
    }
}