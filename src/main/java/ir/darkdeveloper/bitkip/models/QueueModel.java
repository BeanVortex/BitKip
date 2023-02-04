package ir.darkdeveloper.bitkip.models;

import lombok.*;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QueueModel implements Model {
    private int id;
    private String name;
}
