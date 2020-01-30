package be.continuum.tracing.demo.otherservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class OtherController {

    @GetMapping("good")
    public String goodEnpoint() throws InterruptedException {
        Thread.sleep(100);
        return "Remote service called";
    }

    @GetMapping("bad")
    public String badEndpoint() throws InterruptedException {
        Thread.sleep(100);
        throw new RuntimeException("Someone made an bug");
    }

}
