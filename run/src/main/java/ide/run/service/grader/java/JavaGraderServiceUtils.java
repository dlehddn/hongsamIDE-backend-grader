package ide.run.service.grader.java;

import ide.run.domain.ResponseDto;
import ide.run.exception.file.ClassNotFoundedException;
import ide.run.exception.file.FileIOException;
import ide.run.exception.file.MethodNotFoundedException;
import ide.run.exception.file.UrlFormatException;
import ide.run.exception.reflection.NoAuthorityException;
import ide.run.util.enums.PathConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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


@Slf4j
@RequiredArgsConstructor
public class JavaGraderServiceUtils {

    private static final String TMP = PathConstants.TMP.getPath();
    private static final String TMP_SLASH = PathConstants.TMP_SLASH.getPath();
    private static final String INPUT = PathConstants.INPUT.getPath();
    private static final String ANSWER = PathConstants.ANSWER.getPath();
    private static final String CLASS_EXTENSION = PathConstants.CLASS_EXTENSION.getPath();

    protected static CompilationTask getCompiler(File codeFile, DiagnosticCollector<Object> diag) {
        // 컴파일러 세팅 과정
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        File compileResultDirectory = new File(TMP);
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

    protected static URLClassLoader getClassLoader() {
        URLClassLoader classLoader;
        try {
            classLoader = URLClassLoader.newInstance(new URL[]{new File(TMP).toURI().toURL()});
        } catch (MalformedURLException e) {
            log.info("Error By: Class load URI Error", e);
            throw new UrlFormatException(e);
        }
        return classLoader;
    }

    protected static BufferedReader getBufferedReader() {
        BufferedReader ansBr;
        try {
            ansBr = new BufferedReader(new FileReader(TMP_SLASH + ANSWER));
        } catch (FileNotFoundException e) {
            log.info("Error By: BufferedReader IO Error", e);
            throw new FileIOException(e);
        }
        return ansBr;
    }

    protected static String getCurrentAnswer(BufferedReader ansBr) {
        String answer;
        try {
            answer = ansBr.readLine();
        } catch (IOException e) {
            log.info("Error By: BufferedReader IO Error", e);
            throw new FileIOException(e);
        }
        return answer;
    }

    protected static ResponseDto getResponseDto() {
        return ResponseDto.builder()
                .printResult(new ArrayList<>())
                .correctResult(new ArrayList<>())
                .timeResult(new ArrayList<>())
                .isError(false)
                .errorMessage(null)
                .build();
    }

    protected static void settingSystemIn() {
        try {
            System.setIn(new FileInputStream(TMP_SLASH + INPUT));
        } catch (FileNotFoundException e) {
            log.info("Error By : File not founded Error", e);
            throw new FileIOException(e);
        }
    }

    protected static void resourceDelete(String QUESTION_ID) {
        File classFile = new File(TMP, QUESTION_ID + CLASS_EXTENSION);
        if (classFile.exists()) {
            classFile.delete();
        }
    }

    protected static void rollbackSystemIO(PrintStream originOut, InputStream originIn, URLClassLoader classLoader) {
        System.setIn(originIn);
        System.setOut(originOut);
        try {
            classLoader.close();
        } catch (IOException e) {
            log.info("Error By: ClassLoader Close Error", e);
            throw new FileIOException(e);
        }
    }

    protected static Method loadMainMethod(String QUESTION_ID, URLClassLoader classLoader) {
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

    protected static ResponseDto exceptionResponse(ResponseDto result, InvocationTargetException e) {
        result.setError(true);
        result.setErrorMessage(e.getCause().getMessage());
        return result;
    }

    protected static void throwNoAuthorityEx(InputStream originIn, PrintStream originOut, URLClassLoader classLoader, IllegalAccessException e) {
        rollbackSystemIO(originOut, originIn, classLoader);
        log.info("Error By: No Authority On Reflection", e);
        throw new NoAuthorityException(e);
    }

    protected static void writeResponse(ResponseDto result, String answer, ByteArrayOutputStream outputStream, Instant before, Instant after) {
        result.getTimeResult().add(Duration.between(before, after).toMillis());
        result.getPrintResult().add(outputStream.toString().replaceAll("[\\r\\n]", ""));
        result.getCorrectResult().add(outputStream.toString().replaceAll("[\\r\\n]", "").equals(answer));
    }

    protected static ResponseDto compileErrorResponse(DiagnosticCollector<Object> diag) {
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
}
