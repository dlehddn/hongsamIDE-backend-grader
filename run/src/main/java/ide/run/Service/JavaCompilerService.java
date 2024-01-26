package ide.run.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import ide.run.Domain.TestCase;
import ide.run.Domain.UserRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class JavaCompilerService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public void loadInAnsFile(String questionId) throws IOException {
        getS3File(questionId, "input");
        getS3File(questionId, "answer");
//            System.setIn(new FileInputStream("/tmp/input.txt"));
        System.setIn(new FileInputStream("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\input.txt"));
    }

    public void loadTestInAnsFile(String questionId) throws IOException {
        getS3File(questionId, "testCaseInput");
        getS3File(questionId, "testCaseAnswer");
//            System.setIn(new FileInputStream("/tmp/loadTestInAnsFile.txt"));
        System.setIn(new FileInputStream("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\testCaseInput.txt"));
    }

    public UserRunResponse compiler(String questionId) throws Exception {

        // 결과 저장용 리스트
        List<String> result = new ArrayList<>();

        /**
         * Java Compiler API ( .java 파일에서 .class 파일 변형)
         */
//        File javaFile = new File("/tmp/" + questionId + ".java"); // 만들어놓은 .java 파일 불러오는 부분
        File javaFile = new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\" + questionId + ".java");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler(); // 컴파일할 수 있는 컴파일러 모듈만 생성
//        File outputDirectory = new File("/tmp");
        File outputDirectory = new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store");

        Iterable<String> options = Arrays.asList("--release", "11"); // 컴파일러 자바 버전 설정
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputDirectory));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(javaFile));
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>(); // 컴파일 에러 정보를 담을 장소

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diag, options, null, compilationUnits);

        boolean success = task.call();

        // Java Reflection
        if (success) {
            PrintStream originalOut = System.out; // 원래의 표준 출력 보관
            InputStream originalIn = System.in;

            UserRunResponse userRunResponse =  javaReflection(questionId, result);

            System.setIn(originalIn);
            System.setOut(originalOut);

            return userRunResponse;

        } else {   //컴파일 에러 발생
            return getCompileError(diag);
        }

    }

    public UserRunResponse javaReflection(String questionId, List<String> result) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream); // 표준 출력을 ByteArrayOutputStream으로 리다이렉션

        // 컴파일된 .class 파일이 있는 디렉토리를 클래스 로더에 추가
//            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("/tmp").toURI().toURL()});
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store").toURI().toURL()});
        // 클래스 로드
        Class<?> loadedClass = Class.forName(questionId, true, classLoader);
        // 메소드 호출
        Method mainMethod = loadedClass.getMethod("main", String[].class);

        try {
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (Exception e) {
            return new UserRunResponse(e.getCause().toString(), null, null);
        }
        // 클래스 로더 닫기
        classLoader.close();

//        FileOutputStream resultFile = new FileOutputStream("/tmp/output.txt");
        FileOutputStream resultFile = new FileOutputStream("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\output.txt");

        resultFile.write(outputStream.toByteArray());
        resultFile.close();
        outputStream.reset();
        // 사용자의 코드에 대한 실행 결과값이 output.txt로 저장 완료

//            File classFile = new File("/tmp", questionId + ".class");
        File classFile = new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store", questionId + ".class");
        if (classFile.exists()) {
            classFile.delete();
        }

        return new UserRunResponse("채점 성공", result, null);
    }

    public UserRunResponse getCompileError(DiagnosticCollector<JavaFileObject> diag) {
        StringBuilder sb = new StringBuilder();

        for (Diagnostic<? extends JavaFileObject> diagnostic : diag.getDiagnostics()) {
            sb.append("Error on line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ENGLISH) + "\n");
        }
        return new UserRunResponse(sb.toString(), null, null);
    }

    public void getS3File(String questionId, String type) throws IOException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, "admin/" + questionId + "/" + type + ".txt");
//        File file = new File("/tmp/" + type + ".txt");
        File file = new File("C:\\Users\\Seoyeon\\Desktop\\홍삼\\hongsamIDE-backend-grader\\run\\src\\main\\java\\store\\" + type + ".txt");
        amazonS3.getObject(getObjectRequest, file);
    }

    public UserRunResponse compareFiles(UserRunResponse userRunResponse,String outputPath, String answerPath) {

        List<String> result = userRunResponse.getResult();

        try {
            BufferedReader outBr = new BufferedReader(new FileReader(outputPath));
            BufferedReader ansBr = new BufferedReader(new FileReader(answerPath));

            String outLine = outBr.readLine();
            String ansLine = ansBr.readLine();

            // output이 비었을 때 -> 전부 오답
            if (outLine == null) {
                while (ansLine != null) {
                    result.add("오답");
                    ansLine = ansBr.readLine();
                }
                return userRunResponse;
            }
            /**
             * 문제 : 정답 이후 틀린파일 제출하면 이전에 정상처리 .class 파일이 실행되니까, 이거 없애주는 부분 추가! --> ??
             */
            while(outLine != null || ansLine != null) {

                if (outLine != null && !outLine.equals(ansLine)) { // outLine이 null 일 때 equals 예외 발생
                    result.add("오답");
                } else if (outLine != null && outLine.equals(ansLine)) {
                    result.add("정답");
                } else {
                    result.add("오답");
                }
                outLine = outBr.readLine();
                ansLine = ansBr.readLine();
            }
            return userRunResponse;

        } catch (IOException e) {
            e.printStackTrace();
            userRunResponse.setMessage(e.getMessage());
            return userRunResponse;
        }
    }

    public UserRunResponse TestCaseCompareFiles(UserRunResponse userRunResponse, String outputPath, String answerPath, String inputPath) {

        List<String> result = userRunResponse.getResult();
        userRunResponse.setTestCaseResult(new ArrayList<>());
        List<TestCase> testCaseResult = userRunResponse.getTestCaseResult();

        try {
            BufferedReader outBr = new BufferedReader(new FileReader(outputPath));
            BufferedReader ansBr = new BufferedReader(new FileReader(answerPath));
            BufferedReader inputBr = new BufferedReader(new FileReader(inputPath));

            String outLine = outBr.readLine();
            String ansLine = ansBr.readLine();
            String inputLIne = inputBr.readLine();

            // output이 비었을 때 -> 전부 오답
            if (outLine == null) {
                while (ansLine != null) {
                    result.add("오답");
                    testCaseResult.add(new TestCase(inputLIne, ansLine, "오답", ""));
                    ansLine = ansBr.readLine();
                }
                return userRunResponse;
            }
            /**
             * 문제 : 정답 이후 틀린파일 제출하면 이전에 정상처리 .class 파일이 실행되니까, 이거 없애주는 부분 추가! --> ??
             */
            while (outLine != null || ansLine != null) {

                if (outLine != null && !outLine.equals(ansLine)) { // outLine이 null 일 때 equals 예외 발생
                    result.add("오답");
                    testCaseResult.add(new TestCase(inputLIne, ansLine, "오답", outLine));
                } else if (outLine != null && outLine.equals(ansLine)) {
                    result.add("정답");
                    testCaseResult.add(new TestCase(inputLIne, ansLine, "정답", outLine));
                } else {
                    result.add("오답");
                    testCaseResult.add(new TestCase(inputLIne, ansLine, "정답", ""));
                }
                outLine = outBr.readLine();
                ansLine = ansBr.readLine();
            }
            return userRunResponse;

        } catch (IOException e) {
            e.printStackTrace();
            userRunResponse.setMessage(e.getMessage());
            return userRunResponse;
        }
    }

}

