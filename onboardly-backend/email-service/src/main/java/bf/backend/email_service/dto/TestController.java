package bf.backend.email_service.dto;

import bf.backend.email_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final EmailService service;
    @GetMapping("/test")
    public String test(){
        service.sendEmail("anvarprimov07@gmail.com",
                "Test Email",
                "This is a test email from the Email Service!");
        return "Email Service is up and running!";
    }
}
