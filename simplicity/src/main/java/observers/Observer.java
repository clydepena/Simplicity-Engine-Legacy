package observers;

import observers.events.Event;

public interface Observer {
    
    void onNotify(Event event);

}
