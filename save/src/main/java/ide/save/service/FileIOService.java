package ide.save.service;

import ide.save.exception.FileIOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
@Slf4j
public class FileIOService {
    private static final String DEFAULT_PATH = "/tmp/";

    public File makeFileToLambdaMemory (String code, String title, String extension){
        File saveFile = new File(DEFAULT_PATH + title + extension);
        writeCodeToFile(code, saveFile);
        return saveFile;
    }

    private static void writeCodeToFile(String code, File saveFile) {
        try {
            FileWriter fileWriter = new FileWriter(saveFile);
            fileWriter.write(code);
            fileWriter.close();
        } catch (IOException e) {
            log.info("Error By: FileWriter can't load file", e);
            throw new FileIOException(e);
        }
    }
}
