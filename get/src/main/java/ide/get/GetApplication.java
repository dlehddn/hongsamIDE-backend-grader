package ide.get;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
                org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration.class,
        })
public class GetApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetApplication.class, args);
	}

}
