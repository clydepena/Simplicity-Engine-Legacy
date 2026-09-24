package observers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import observers.events.Event;
import observers.events.EventType;

public class EventSystem {
    
    public static void addObserver(Observer observer) {
        observers.add(observer);
    }
    
    public static void notify(Event event) {
        for(Observer observer : observers) {
            observer.onNotify(event);
        }
    }
    
    private static final List<Observer> observers = new ArrayList<>();
    private static final Queue<Event> queue = new LinkedList<>();
    private static final LinkedList<AtomicReference<Event>> uniqueQueue = new LinkedList<>();
    private static final Map<EventType, AtomicReference<Event>> set = new HashMap<>();
    private static final Queue<Boolean> orderQueue = new LinkedList<>();

    public static void publish(Event item) {
        queue.add(item);
        orderQueue.add(false);
    }

    public static void publishCoalescing(Event event) {
        if (set.containsKey(event.type)) {
            set.get(event.type).set(event);
        } else {
            AtomicReference<Event> ref = new AtomicReference<>(event);
            uniqueQueue.add(ref);
            set.put(event.type, ref);
            orderQueue.add(true);
        }
    }

    public static Event poll() {
        if (orderQueue.isEmpty()) return null;
        Boolean result = orderQueue.poll();
        if (result == null) return null;
        if (result) {
            AtomicReference<Event> ref = uniqueQueue.poll();
            if (ref == null) return null;
            Event event = ref.get();
            if (event != null) {
                set.remove(event.type);
            }
            return event;
        } else {
            return queue.poll();
        }
    }

    public static void processEvents() {
        List<Event> eventsToProcess = new ArrayList<>();
        while (true) {
            Event event = poll();
            if (event == null) break;
            eventsToProcess.add(event);
        }

        for (Event event : eventsToProcess) {
            for (Observer observer : observers) {
                if (event.stopPropagate) break;
                observer.onNotify(event);
            }
            event.onEnd();
            // System.out.println(event);
        }
    }
}