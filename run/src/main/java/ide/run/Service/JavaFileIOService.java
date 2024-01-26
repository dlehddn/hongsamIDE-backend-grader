package ide.run.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
@Slf4j
public class JavaFileIOService {

    public File saveFile(String newCode, String title) throws IOException {
//        File javaFile = new File("/tmp/" + title + ".java");
//        File javaFile = new File("C:\\Users\\Seoyeon\\AppData\\Local\\Temp\\" + title + ".java");
        File javaFile = new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\" + title + ".java");
        FileWriter writer = new FileWriter(javaFile);
        writer.write(newCode);
        writer.close();
        return javaFile;
    }

    public String findFileByName(String title) throws IOException {
        File javaFile = new File("/tmp/" + title + ".java");
        BufferedReader br = new BufferedReader(new FileReader(javaFile));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        log.info(String.valueOf(sb));
        return sb.toString();
    }
}
