package eu.cqse.teamscale.client;

import okhttp3.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

public class JenkinsConsoleInterceptor implements Interceptor {

    private PrintStream stream;

    /**
     * Constructor
     */
    public JenkinsConsoleInterceptor(PrintStream stream) {
        this.stream = stream;
    }

    @Nonnull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long requestStartTime = System.nanoTime();
        stream.println(request.method());
        stream.println(request.url());
        stream.println(request.headers());

        Response response = getResponse(chain, request, stream);

        long requestEndTime = System.nanoTime();
        stream.println(String
                .format("<-- Received response for %s %s in %.1fms%n%s%n%n", response.code(),
                        response.request().url(), (requestEndTime - requestStartTime) / 1e6d, response.headers()));
        ResponseBody wrappedBody = null;
        if (response.body() != null) {
            MediaType contentType = response.body().contentType();
            String content = response.body().string();
            stream.println(content);

            wrappedBody = ResponseBody.create(contentType, content);
        }
        return response.newBuilder().body(wrappedBody).build();
    }


    private Response getResponse(Chain chain, Request request, PrintStream stream) throws IOException {
        try {
            return chain.proceed(request);
        } catch (Exception e) {
            stream.println("\n\nRequest failed!\n");
            stream.println(e);
            throw e;
        }
    }
}
