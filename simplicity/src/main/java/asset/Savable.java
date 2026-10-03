package asset;

import java.io.IOException;

public interface Savable {
    String displayName();
    void save() throws IOException;
}
