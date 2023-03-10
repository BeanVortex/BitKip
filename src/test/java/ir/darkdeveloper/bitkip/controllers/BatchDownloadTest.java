package ir.darkdeveloper.bitkip.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchDownloadTest {

    @Test
    void generateLinks1Digits() {
        var batchDown = new BatchDownload();
        int start = 1;
        int end = 9;
        var l = batchDown.generateLinks("https://www.file.com/file$$$$.so", start, end, 5, false);
        for (int i = 0, j = 0; i < end; i++, j++)
            assertEquals(l.get(j).getLink(), "https://www.file.com/file%d%d%d%d.so".formatted(0, 0, 0, i + 1));

    }

    @Test
    void generateLinks2Digits() {
        var batchDown = new BatchDownload();
        int start = 5;
        int end = 16;
        var l = batchDown.generateLinks("https://www.file.com/file$$$$.so", start, end, 5, false);
        for (int i = start - 1, j = 0; i < end; i++, j++) {
            if ((i + 1) / 10 > 0)
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d%d%d.so".formatted(0, 0, i + 1));
            else
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d%d%d%d.so".formatted(0, 0, 0, i + 1));
        }
    }

    @Test
    void generateLinks3Digits() {
        var batchDown = new BatchDownload();
        int start = 56;
        int end = 126;
        var l = batchDown.generateLinks("https://www.file.com/file$$$.so", start, end, 5, false);
        for (int i = start - 1, j = 0; i < end; i++, j++) {
            if ((i + 1) / 100 > 0)
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d.so".formatted(i + 1));
            else
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d%d.so".formatted(0, i + 1));
        }
    }

    @Test
    void generateLinks4Digits() {
        var batchDown = new BatchDownload();
        int start = 926;
        int end = 1260;
        var l = batchDown.generateLinks("https://www.file.com/file$$$$.so", start, end, 5, false);
        for (int i = start - 1, j = 0; i < end; i++, j++) {
            if ((i + 1) / 1000 > 0)
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d.so".formatted(i + 1));
            else
                assertEquals(l.get(j).getLink(), "https://www.file.com/file%d%d.so".formatted(0, i + 1));
        }
    }

    @Test
    void generateLinks3Digits2Patterns() {
        var batchDown = new BatchDownload();
        int start = 56;
        int end = 126;
        assertThrows(IllegalArgumentException.class,
                () -> batchDown.generateLinks("https://www.file.com/file$$.so", start, end, 5, false),
                "Not enough pattern to cover the range");
    }
}