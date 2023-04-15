package ir.darkdeveloper.bitkip.config;

import java.util.ArrayList;
import java.util.List;

public class QueueSubject {
    private final List<QueueObserver> observers = new ArrayList<>();

    public void addObserver(QueueObserver o) {
        observers.add(o);
    }

    public void removeObserver(QueueObserver o){
        observers.remove(o);
    }

    public void notifyAllObservers() {
        observers.forEach(QueueObserver::updateQueue);
    }
}
