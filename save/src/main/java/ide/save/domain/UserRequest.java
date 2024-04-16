package ide.save.domain;


import lombok.Getter;

@Getter
public class UserRequest {
    private String questionId;
    private String uuid;
    private String requestCode;
    private String language;
}
