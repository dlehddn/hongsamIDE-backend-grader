package ide.run.util.enums;

import lombok.Getter;

@Getter
public enum PathConstants {
    TMP("/tmp"),
    TMP_SLASH("/tmp/"),
    INPUT("input.txt"),
    ANSWER("answer.txt"),
    CLASS_EXTENSION(".class");

    private final String path;

    PathConstants(String path) {
        this.path = path;
    }
}
