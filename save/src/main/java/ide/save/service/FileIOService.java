package ide.save.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
@Slf4j
public class FileIOService {
    private static final String DEFAULT_PATH = "/tmp/";

    public File makeFileToLambdaMemory (String code, String title, String extension) throws IOException {
        File saveFile = new File(DEFAULT_PATH + title + extension);
        writeCodeToFile(code, saveFile);
        return saveFile;
    }

    private static void writeCodeToFile(String code, File saveFile) throws IOException {
        FileWriter fileWriter = new FileWriter(saveFile);
        fileWriter.write(code);
        fileWriter.close();
    }
}
