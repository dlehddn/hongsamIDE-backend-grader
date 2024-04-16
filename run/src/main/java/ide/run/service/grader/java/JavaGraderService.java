package ide.run.service.grader.java;

import ide.run.domain.RequestDto;
import ide.run.domain.ResponseDto;
import ide.run.repository.S3Repository;
import ide.run.service.grader.GraderService;
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
        // 요청 코드 저장
        s3Repository.saveUserCode(USER_ID, codeFile);

        // 컴파일 에러 검출을 위한 DiagnosticCollector
        // 컴파일러 생성
        DiagnosticCollector<Object> diag = new DiagnosticCollector<>();
        CompilationTask compiler = getCompiler(codeFile, diag);

        if (compiler.call()) {
            // 입력 파일, 정답 파일 불러오기
            loadAdminFile(QUESTION_ID);

            // 세팅 변수 생성
            ResponseDto result = getResponseDto();
            BufferedReader ansBr = getBufferedReader();
            InputStream originIn = System.in;
            PrintStream originOut = System.out;
            ByteArrayOutputStream refOutputStream = new ByteArrayOutputStream();
            PrintStream refSystemOut = new PrintStream(refOutputStream);
            settingSystemIO(refSystemOut);

            // 메인 메소드 로드
            URLClassLoader classLoader = getClassLoader();
            Method mainMethod = loadMainMethod(QUESTION_ID, classLoader);

            // 테스트 케이스 별 채점 시작
            int testCase = GRADE_TYPE.equals("all") ? 10 : 2;
            for (int i = 0; i < testCase; i++) {
                String answer = getCurrentAnswer(ansBr);
                try {
                    Instant beforeTime = Instant.now();
                    mainMethod.invoke(null, (Object) new String[]{});
                    Instant afterTime = Instant.now();
                    writeResponse(result, answer, refOutputStream, beforeTime, afterTime);
                } catch (IllegalAccessException e) {
                    throwNoAuthorityEx(originIn, originOut, classLoader, e);
                } catch (InvocationTargetException e) {
                    exceptionResponse(result, e, answer);
                }
                refOutputStream.reset();
            }
            // SystemIO rollback
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