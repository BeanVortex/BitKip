package io.beanvortex.bitkip.models;

import javafx.scene.control.MenuItem;

import java.util.Objects;

public record StartedQueue(QueueModel queue, MenuItem startItem, MenuItem stopItem) {

    public StartedQueue(QueueModel queue) {
        this(queue, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StartedQueue that = (StartedQueue) o;
        return Objects.equals(queue, that.queue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queue);
    }
}