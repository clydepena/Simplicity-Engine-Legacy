package observers.events;

public class Event {
    public EventType type;
    public Object obj;

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
}
