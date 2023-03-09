package ir.darkdeveloper.bitkip.config;

import ir.darkdeveloper.bitkip.models.QueueModel;

import java.util.ArrayList;
import java.util.List;

public class QueueSubject {
    private static final List<QueueObserver> observers = new ArrayList<>();

    public void addObserver(QueueObserver o) {
        observers.add(o);
    }

    public void notifyAllObservers() {
        observers.forEach(QueueObserver::updateQueue);
    }

}
