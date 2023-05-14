package ir.darkdeveloper.bitkip.models;

import lombok.*;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueueModel {
    private int id;
    private String name;
    private boolean editable;
    private boolean canAddDownload;
    private boolean hasFolder;
    private boolean downloadFromTop = true;
    private String speed = "0";
    private int simultaneouslyDownload = 1;
    private ScheduleModel schedule;
    private CopyOnWriteArrayList<DownloadModel> downloads;

    public QueueModel(String name, boolean canAddDownload) {
        this.name = name;
        this.canAddDownload = canAddDownload;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (QueueModel) o;
        return id == that.id && editable == that.editable &&
                canAddDownload == that.canAddDownload && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, editable, canAddDownload);
    }

    public boolean hasFolder() {
        return hasFolder;
    }

    public String toStringModel() {
        return "QueueModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", editable=" + editable +
                ", canAddDownload=" + canAddDownload +
                ", hasFolder=" + hasFolder +
                ", downloadFromTop=" + downloadFromTop +
                ", speed='" + speed + '\'' +
                ", simultaneouslyDownload=" + simultaneouslyDownload +
                ", schedule=" + schedule +
                '}';
    }
}
