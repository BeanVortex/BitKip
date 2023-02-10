package ir.darkdeveloper.bitkip.models;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueueModel {
    private int id;
    private String name;
    private boolean editable;
    private boolean canAddDownload;


    public QueueModel(String name, boolean editable, boolean canAddDownload) {
        this.name = name;
        this.editable = editable;
        this.canAddDownload = canAddDownload;
    }

    @Override
    public String toString() {
        return name;
    }
}
