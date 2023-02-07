package ir.darkdeveloper.bitkip.models;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueueModel implements Model {
    private int id;
    private String name;
//    private boolean displayableInDownloadList;


    @Override
    public String toString() {
        return name;
    }
}
