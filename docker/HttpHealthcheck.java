import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal dependency-free HTTP health check for JRE-only container images.
 */
public final class HttpHealthcheck {

    private HttpHealthcheck() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: HttpHealthcheck <url>");
            System.exit(2);
        }

        try {
            var request = HttpRequest.newBuilder(URI.create(args[0]))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            var response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.exit(1);
            }
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
