package be.continuum.tracing.demo.entryservice;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController("/")
public class EntryController {

    private final Tracer tracer;
    private final RestTemplate restTemplate;

    @Autowired
    public EntryController(Tracer tracer, RestTemplate restTemplate) {
        this.tracer = tracer;
        this.restTemplate = restTemplate;
    }

    @GetMapping("")
    public String welcome() {
        return "<img src=\"https://media1.tenor.com/images/b365e7d26fe05de381a4fdfd9d8f9517/tenor.gif?itemid=5220612\" />";
    }

    @GetMapping("simple")
    public String simple() {
        return "Let's trace something";
    }

    @GetMapping("manual")
    public String manual() throws InterruptedException {
        Span span = tracer.buildSpan("manual-span").start();
        span.setTag("Foo", "Bar");
        try(Scope scope = tracer.activateSpan(span)) {
            Thread.sleep(100);
            return "We created a manual trace in here";
        }
        finally {
            span.finish();
        }
    }

    @GetMapping("remote")
    public String remote() {
        String endpoint = selectEndpoint();
        ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
        return response.getBody();
    }

    @GetMapping("bad-thread")
    public String badThread() throws ExecutionException, InterruptedException {
        CompletableFuture<String> resultFuture = CompletableFuture.supplyAsync(() -> {
            Span span = tracer.buildSpan("bad-span").start();
            try(Scope scope = tracer.activateSpan(span)) {
                Thread.sleep(1000);
                return "This took a while";
            } catch (InterruptedException ignored) {
                return "It broke";
            }
            finally {
                span.finish();
            }
        });
        return resultFuture.get();
    }

    @GetMapping("good-thread")
    public String goodThread() throws ExecutionException, InterruptedException {
        Span currentSpan = tracer.activeSpan();
        CompletableFuture<String> resultFuture = CompletableFuture.supplyAsync(() -> {
            Span span = tracer.buildSpan("good-span").asChildOf(currentSpan).start();
            try(Scope scope = tracer.activateSpan(span)) {
                Thread.sleep(1000);
                return "This took a while";
            } catch (InterruptedException ignored) {
                return "It broke";
            }
            finally {
                span.finish();
            }
        });
        return resultFuture.get();
    }

    private String selectEndpoint() {
        if(Math.random() < 0.5) {
            return "http://localhost:8081/good";
        }
        return "http://localhost:8081/bad";
    }

}
