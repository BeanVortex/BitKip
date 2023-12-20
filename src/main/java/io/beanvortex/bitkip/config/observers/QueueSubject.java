package io.beanvortex.bitkip.config.observers;

import io.beanvortex.bitkip.models.QueueModel;

import java.util.ArrayList;
import java.util.List;

public class QueueSubject {

    private static final QueueSubject queueSubject = new QueueSubject();
    private static final List<QueueModel> queues = new ArrayList<>();

    private QueueSubject() {

    }

    private final List<QueueObserver> observers = new ArrayList<>();

    public void addObserver(QueueObserver o) {
        observers.add(o);
    }

    public void removeObserver(QueueObserver o) {
        observers.remove(o);
    }

    private void notifyAllObservers() {
        observers.forEach(QueueObserver::updateQueue);
    }


    public static void addQueue(QueueModel queue) {
        queues.add(queue);
        queueSubject.notifyAllObservers();
    }

    public static void deleteQueue(String name) {
        getQueues().removeIf(qm -> qm.getName().equals(name));
        queueSubject.notifyAllObservers();
    }

    public static List<QueueModel> getQueues() {
        return queues;
    }

    public static void addAllQueues(List<QueueModel> queues) {
        QueueSubject.queues.clear();
        QueueSubject.queues.addAll(queues);
        queueSubject.notifyAllObservers();
    }

    public static QueueSubject getQueueSubject() {
        return queueSubject;
    }
}
