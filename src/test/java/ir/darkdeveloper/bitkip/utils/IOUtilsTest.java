package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.config.AppConfigs;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

class IOUtilsTest {

    @Test
    void moveAndDeletePreviousData() throws IOException {
        var path = System.getProperty("user.home")
                + File.separator + "Desktop"
                + File.separator + "BitKip-test-folder" + File.separator;
        var newPath = System.getProperty("user.home")
                + File.separator + "Desktop"
                + File.separator + "BitKip-test-folder-new" + File.separator;

        AppConfigs.log = LoggerFactory.getLogger("BitKip");

        IOUtils.mkdir(path);
        for (int i = 0; i < 10; i++) {
            var file = new File(path + "file" + i);
            if (!file.exists())
                file.createNewFile();
        }
        IOUtils.mkdir(path + "folder1");

        for (int i = 0; i < 10; i++) {
            var file = new File(path + "folder1" + File.separator + "file" + i);
            if (!file.exists())
                file.createNewFile();
        }

        IOUtils.moveAndDeletePreviousData(path, newPath);
        var prevFolder = new File(path);
        var prevFolderWithIn = new File(path + "folder1");
        var newFolder = new File(newPath);
        var newFolderWithIn = new File(newPath + "folder1");

        assert !prevFolder.exists();
        assert !prevFolderWithIn.exists();
        assert newFolder.exists();
        assert newFolderWithIn.exists();
        assert newFolder.listFiles().length == 11;
        assert newFolderWithIn.listFiles().length == 10;

        IOUtils.deleteFolderWithContent(newPath);
        assert !newFolder.exists();
        assert !newFolderWithIn.exists();
    }
}