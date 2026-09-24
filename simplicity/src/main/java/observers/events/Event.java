package observers.events;

public class Event {
    public EventType type = EventType.Unset;
    public Object obj;
    public boolean stopPropagate = false;

    public Event(EventType type) {
        this.type = type;
        this.obj = null;
    }

    public Event() {
        this.type = EventType.UserEvent;
        this.obj = null;
    }

    public Event(EventType type, Object obj) {
        this.type = type;
        this.obj = obj;
    }

    public Object getObject() {
        return this.obj;
    }

    public void stopPropagate() {
        stopPropagate = true;
    }

    public void onEnd() {
        obj = null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "'" + type + "'";
    }
}
