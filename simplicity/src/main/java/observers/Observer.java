package observers;

import observers.events.Event;
import simplicity.GameObject;

public interface Observer {
    
    void onNotify(Event event);

}
