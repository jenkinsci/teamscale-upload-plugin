package eu.cqse.teamscale.client;

import eu.cqse.teamscale.jenkins.upload.TeamscaleUploadBuilder;
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
        stream.print(TeamscaleUploadBuilder.INFO + request.method() + " - ");
        stream.println(request.url());

        Response response = getResponse(chain, request, stream);

        long requestEndTime = System.nanoTime();

        double requestTimeInMs = (requestEndTime - requestStartTime) / 1e6d;

        if(response.code() < 200 || response.code() > 399){
            stream.println(TeamscaleUploadBuilder.ERROR + String.format("Response - %s in %.1fms", response.message(), requestTimeInMs));
        }else{
            stream.println(TeamscaleUploadBuilder.INFO + String.format("Response - %s in %.1fms", response.code(), requestTimeInMs ));
        }

        return response.newBuilder().body(response.body()).build();
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
