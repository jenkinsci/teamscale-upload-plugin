package eu.cqse.teamscale.client;

import eu.cqse.teamscale.jenkins.upload.TeamscaleUploadBuilder;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class for forwarding HTTP-API-responses of Teamscale Uploader to the Jenkins Console.
 */
public class JenkinsConsoleInterceptor implements Interceptor {

    private PrintStream stream;

    /**
     * Constructor for intercepting the console output.
     * @param stream printstream of jenkins console.
     */
    public JenkinsConsoleInterceptor(PrintStream stream) {
        this.stream = stream;
    }

    @Nonnull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long requestStartTime = System.nanoTime();
        stream.print(TeamscaleUploadBuilder.INFO + request.method() + " - ");
        stream.println(request.url());

        Response response = sendRequest(chain, request, stream);

        long requestEndTime = System.nanoTime();

        double requestTimeInMs = (requestEndTime - requestStartTime) / 1e6d;

        if(response.code() < 200 || response.code() > 399){
            ResponseBody body = response.body();
            stream.println(TeamscaleUploadBuilder.ERROR + String.format("Response - %s %s in %.1fms body:%n%s", response.code(), response.message(), requestTimeInMs, body != null ? body.string() : "Empty"));
        }else{
            stream.println(TeamscaleUploadBuilder.INFO + String.format("Response - %s in %.1fms", response.code(), requestTimeInMs ));
        }

        return response.newBuilder().body(response.body()).build();
    }


    /**
     * Send intercepted request and forward IO-exceptions.
     *
     * @param chain which is intercepted.
     * @param request to make.
     * @param stream to forward IO-exception to.
     * @return response of request.
     * @throws IOException which occurred.
     */
    private Response sendRequest(Chain chain, Request request, PrintStream stream) throws IOException {
        try {
            return chain.proceed(request);
        } catch (IOException e) {
            stream.println("\n\nRequest failed!\n");
            stream.println(e);
            throw e;
        }
    }
}
