package ide.run.service.grader.java;

import ide.run.domain.RequestDto;
import ide.run.domain.ResponseDto;
import ide.run.repository.S3Repository;
import ide.run.service.grader.GraderService;
import ide.run.util.enums.PathConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.time.Instant;

import static ide.run.service.grader.java.JavaGraderServiceUtils.*;
import static javax.tools.JavaCompiler.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaGraderService implements GraderService {

    private final S3Repository s3Repository;

    @Override
    public ResponseDto grader(File codeFile, RequestDto requestDto) {
        final String USER_ID = requestDto.getUuid();
        final String QUESTION_ID = requestDto.getQuestionId();
        final String GRADE_TYPE = requestDto.getGradeType();

        s3Repository.saveUserCode(USER_ID, codeFile);

        DiagnosticCollector<Object> diag = new DiagnosticCollector<>();
        CompilationTask compiler = getCompiler(codeFile, diag);

        if (compiler.call()) {
            loadAdminFile(QUESTION_ID);

            ResponseDto result = getResponseDto();
            BufferedReader ansBr = getBufferedReader();
            InputStream originIn = System.in;
            PrintStream originOut = System.out;

            settingSystemIn();

            URLClassLoader classLoader = getClassLoader();
            Method mainMethod = loadMainMethod(QUESTION_ID, classLoader);

            int testCase = GRADE_TYPE.equals("all") ? 10 : 2;
            for (int i = 0; i < testCase; i++) {
                String answer = getCurrentAnswer(ansBr);
                ByteArrayOutputStream refOutputStream = new ByteArrayOutputStream();
                PrintStream refSystemOut = new PrintStream(refOutputStream);
                System.setOut(refSystemOut);
                try {
                    Instant beforeTime = Instant.now();
                    mainMethod.invoke(null, (Object) new String[]{});
                    Instant afterTime = Instant.now();
                    writeResponse(result, answer, refOutputStream, beforeTime, afterTime);
                } catch (IllegalAccessException e) {
                    throwNoAuthorityEx(originIn, originOut, classLoader, e);
                } catch (InvocationTargetException e) {
                    return exceptionResponse(result, e);
                }
            }
            rollbackSystemIO(originOut, originIn, classLoader);
            resourceDelete(QUESTION_ID);
            return result;
        } else {
            return compileErrorResponse(diag);
        }
    }

    private void loadAdminFile(String QUESTION_ID) {
        s3Repository.loadAdminFile(QUESTION_ID, ANSWER);
        s3Repository.loadAdminFile(QUESTION_ID, INPUT);
    }
}