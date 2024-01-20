package ide.run.service;

import ide.run.domain.RequestDto;
import ide.run.domain.ResponseDto;
import ide.run.exception.compile.CompileErrorException;
import ide.run.exception.file.ClassNotFoundedException;
import ide.run.exception.file.FileIOException;
import ide.run.exception.file.MethodNotFoundedException;
import ide.run.exception.file.UrlFormatException;
import ide.run.exception.reflection.NoAuthorityException;
import ide.run.exception.reflection.OnReflectionException;
import ide.run.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static javax.tools.JavaCompiler.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaGraderService implements GraderService {

    private final S3Repository s3Repository;
    private final String LAMBDA_PATH = "/tmp";
    private final String SLASH = "/";
    private final String INPUT = "input.txt";
    private final String OUTPUT = "output.txt";
    private final String ANSWER = "answer.txt";
    private final String CLASS_EXTENSION = ".class";

    @Override
    public ResponseDto grader(File codeFile, RequestDto requestDto) {
        final String USER_ID = requestDto.getUuid();
        final String QUESTION_ID = requestDto.getQuestionId();
        final String GRADE_TYPE = requestDto.getGradeType();

        //S3에 파일 저장
        s3Repository.saveUserCode(USER_ID, codeFile);

        //컴파일
        DiagnosticCollector<Object> diag = new DiagnosticCollector<>();
        CompilationTask compiler = getCompiler(codeFile, LAMBDA_PATH, diag);

        //컴파일 성공
        if (compiler.call()) {
            // Input, Answer 파일 S3에서 불러오기
            loadAdminFile(QUESTION_ID);

            // 필요한 변수 설정
            PrintStream originOut = System.out;
            InputStream originIn = System.in;
            ByteArrayOutputStream newOutputStream = new ByteArrayOutputStream();
            PrintStream newPrintStream = new PrintStream(newOutputStream);

            // System IO 세팅
            systemIOSetting(newPrintStream);

            // ClassLoader 세팅
            URLClassLoader classLoader = getClassLoader();

            // 실행할 메인 메소드 얻어오고 실행
            Method mainMethod = loadMainMethod(QUESTION_ID, classLoader);

            // 시간 출력을 위한 개선 필요
            try {
                mainMethod.invoke(null, (Object) new String[]{});
            } catch (IllegalAccessException e) {
                rollbackSystemIO(originOut, originIn, classLoader);
                log.info("Error By: No Authority On Reflection", e);
                throw new NoAuthorityException(e);
            } catch (InvocationTargetException e) {
                return ResponseDto.builder()
                        .isError(true)
                        .errorMessage(e.getCause().getMessage())
                        .build();
            }

            rollbackSystemIO(originOut, originIn, classLoader);
            makeResultFile(newOutputStream);
            resourceDelete(QUESTION_ID);

            return getResponseDto();

        }
        //컴파일 실패
        else {
            return occurCompileError(diag);
        }
    }

    private ResponseDto getResponseDto() {
        ResponseDto responseDto = ResponseDto.builder()
                .printResult(new ArrayList<>())
                .correctResult(new ArrayList<>())
                .build();

        BufferedReader outBr;
        BufferedReader ansBr;
        try {
            outBr = new BufferedReader(new FileReader(LAMBDA_PATH + SLASH + OUTPUT));
            ansBr = new BufferedReader(new FileReader(LAMBDA_PATH + SLASH + ANSWER));
        } catch (FileNotFoundException e) {
            log.info("Error By: File Not Found Error");
            throw new FileIOException(e);
        }

        try {
            String outLine = outBr.readLine();
            String ansLine = ansBr.readLine();
            // 출력을 안했을 경우
            if (outLine == null) {
                while (ansLine != null) {
                    responseDto.getCorrectResult().add(false);
                    responseDto.getPrintResult().add("출력값이 없습니다.");
                    ansLine = ansBr.readLine();
                }
            }
            while (outLine != null && ansLine != null) {
                if (!outLine.equals(ansLine)) {
                    responseDto.getCorrectResult().add(false);
                } else {
                    responseDto.getCorrectResult().add(true);
                }
                responseDto.getPrintResult().add(outLine);
                outLine = outBr.readLine();
                ansLine = ansBr.readLine();
            }
            return responseDto;
        } catch (IOException e) {
            log.info("Error By: File IO Exception Error");
            throw new FileIOException(e);
        }
    }

    private void resourceDelete(String QUESTION_ID) {
        File classFile = new File(LAMBDA_PATH, QUESTION_ID + CLASS_EXTENSION);
        if (classFile.exists()) {
            classFile.delete();
        }
    }

    private static void makeResultFile(ByteArrayOutputStream newOutputStream) {
        FileOutputStream resultFile;
        try {
            resultFile = new FileOutputStream("/tmp/output.txt");
        } catch (FileNotFoundException e) {
            log.info("Error By: File Not Found Error", e);
            throw new FileIOException(e);
        }
        try {
            resultFile.write(newOutputStream.toByteArray());
            resultFile.close();
        } catch (IOException e) {
            log.info("Error By: File Write or Close Error", e);
            throw new FileIOException(e);
        }
        newOutputStream.reset();
    }

    private static void rollbackSystemIO(PrintStream originOut, InputStream originIn, URLClassLoader classLoader) {
        System.setIn(originIn);
        System.setOut(originOut);
        try {
            classLoader.close();
        } catch (IOException e) {
            log.info("Error By: ClassLoader Close Error", e);
            throw new FileIOException(e);
        }
    }

    private static Method loadMainMethod(String QUESTION_ID, URLClassLoader classLoader) {
        Class<?> loadedClass;
        try {
            loadedClass = Class.forName(QUESTION_ID, true, classLoader);
        } catch (ClassNotFoundException e) {
            log.info("Error By: Class Not Found Error", e);
            throw new ClassNotFoundedException(e);
        }
        try {
            return loadedClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            log.info("Error By: Method Not Found Error", e);
            throw new MethodNotFoundedException(e);
        }
    }

    private URLClassLoader getClassLoader() {
        URLClassLoader classLoader;
        try {
            classLoader = URLClassLoader.newInstance(new URL[]{new File(LAMBDA_PATH).toURI().toURL()});
        } catch (MalformedURLException e) {
            log.info("Error By: Class load URI Error", e);
            throw new UrlFormatException(e);
        }
        return classLoader;
    }

    private void systemIOSetting(PrintStream newPrintStream) {
        System.setOut(newPrintStream);
        try {
            System.setIn(new FileInputStream(LAMBDA_PATH + SLASH + INPUT));
        } catch (FileNotFoundException e) {
            log.info("Error By: File Not Found Error", e);
            throw new FileIOException(e);
        }
    }


    private void loadAdminFile(String QUESTION_ID) {
        s3Repository.loadAdminFile(QUESTION_ID, ANSWER);
        s3Repository.loadAdminFile(QUESTION_ID, INPUT);
    }

    private static ResponseDto occurCompileError(DiagnosticCollector<Object> diag) {
        ResponseDto responseDto = ResponseDto.builder()
                .isError(true)
                .build();
        StringBuilder sb = new StringBuilder();
        diag.getDiagnostics().stream()
                .forEach(info -> sb.append("Error on line " + info.getLineNumber() + ": "
                        + info.getMessage(Locale.ENGLISH) + "\n"));
        responseDto.setErrorMessage(sb.toString());
        return responseDto;
    }

    private static CompilationTask getCompiler(File codeFile, String LAMBDA_PATH, DiagnosticCollector<Object> diag) {
        // 컴파일러 세팅 과정
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        File compileResultDirectory = new File(LAMBDA_PATH);
        Iterable<String> options = Arrays.asList("--release", "11");
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(compileResultDirectory));
        } catch (IOException e) {
            log.info("Error by: compile", e);
            throw new FileIOException(e);
        }
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(codeFile));
        // 컴파일러 반환
        return compiler.getTask(null, fileManager, diag, options, null, compilationUnits);
    }
}
