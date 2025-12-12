package neusta.shortly;

import org.springframework.boot.SpringApplication;

public class TestShortlyApplication {

    static void main(final String[] args) {
        SpringApplication.from(ShortlyApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
