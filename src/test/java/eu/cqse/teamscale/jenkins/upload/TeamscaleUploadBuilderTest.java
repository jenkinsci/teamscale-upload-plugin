package eu.cqse.teamscale.jenkins.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.HttpUrl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;

class TeamscaleUploadBuilderTest {

    private static class Constants {
        private static final String url = "http://localhost:8100";
        private static final String teamscaleProject = "jenkinsplugin";
        private static final String partition = "simple";
        private static final String uploadMessage = "Uploaded simple coverage";
        private static final String fileFormat = "**/*.simple";
        private static final String reportFormatId = "SIMPLE";
        private static final String revision = "1337";
        private static final String repository = "repository";
        private static final String username = "my-user";
        private static final String password = "wonderfulPassword";
        private static final String reportFile = "test.simple";
        private static final String reportContent = "RunExec.java\n8-10";
        private static final String uploadPath =
                "/api/v2024.7.0/projects/" + teamscaleProject + "/external-analysis/session/auto-create/report";
    }

    @RegisterExtension
    private final RealJenkinsExtension extension = new RealJenkinsExtension();

    @ParameterizedTest
    @ValueSource(strings = {"a"})
    @NullSource
    public void testConfigRoundtrip(@Nullable String repository) throws Throwable {
        extension.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            TeamscaleUploadBuilder teamscaleUpload1 = new TeamscaleUploadBuilder(
                    Constants.url,
                    "teamscale_id",
                    Constants.teamscaleProject,
                    Constants.partition,
                    Constants.uploadMessage,
                    Constants.fileFormat,
                    Constants.reportFormatId,
                    "");
            teamscaleUpload1.setRepository(repository);
            project.getPublishersList().add(teamscaleUpload1);
            project = jenkins.configRoundtrip(project);
            TeamscaleUploadBuilder teamscaleUpload2 = new TeamscaleUploadBuilder(
                    Constants.url,
                    "teamscale_id",
                    Constants.teamscaleProject,
                    Constants.partition,
                    Constants.uploadMessage,
                    Constants.fileFormat,
                    Constants.reportFormatId,
                    "");
            teamscaleUpload2.setRepository(repository);
            jenkins.assertEqualDataBoundBeans(
                    teamscaleUpload2, project.getPublishersList().get(0));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"a"})
    @NullSource
    public void testPipelineWithoutCredentials(@Nullable String repository) throws Throwable {
        extension.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.setScm(new SingleFileSCM("test.simple", "RunExec.java\n8-10"));
            TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(
                    Constants.url,
                    "teamscale_id",
                    Constants.teamscaleProject,
                    Constants.partition,
                    Constants.uploadMessage,
                    Constants.fileFormat,
                    Constants.reportFormatId,
                    "");
            publisher.setRepository(repository);
            project.getPublishersList().add(publisher);

            FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
            jenkins.assertLogContains("credentials are null", build);
        });
    }

    @Test
    public void testPipelineWithCredentials() throws Throwable {
        extension.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.setScm(new SingleFileSCM("test.simple", "RunExec.java\n8-10"));
            TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(
                    Constants.url,
                    "teamscale_id",
                    Constants.teamscaleProject,
                    Constants.partition,
                    Constants.uploadMessage,
                    Constants.fileFormat,
                    Constants.reportFormatId,
                    "1337");
            publisher.setRepository("repository");
            UsernamePasswordCredentialsImpl usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    "username-pass",
                    "Username / Password credential for testing",
                    "my-user",
                    "wonderfulPassword");
            SystemCredentialsProvider.getInstance().getCredentials().add(usernamePasswordCredentials);
            publisher.setCredentialsId("username-pass");
            publisher.setResultOnUploadFailure("FAILURE");
            project.getPublishersList().add(publisher);

            FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

            jenkins.assertLogContains(
                    "TS-ERROR: Failed to upload reports to Teamscale: Failed to connect to localhost", build);
        });
    }

    @Test
    public void testPipelineWithUnsuccessfulHttpResponse() throws Throwable {
        extension.then(jenkins -> {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.setScm(new SingleFileSCM("test.simple", "RunExec.java\n8-10"));
            TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(
                    jenkins.getURL().toString(), // use a responsive http server that will fail with an http status code
                    "teamscale_id",
                    Constants.teamscaleProject,
                    Constants.partition,
                    Constants.uploadMessage,
                    Constants.fileFormat,
                    Constants.reportFormatId,
                    "1337");
            publisher.setRepository("repository");
            UsernamePasswordCredentialsImpl usernamePasswordCredentials = new UsernamePasswordCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    "username-pass",
                    "Username / Password credential for testing",
                    "my-user",
                    "wonderfulPassword");
            SystemCredentialsProvider.getInstance().getCredentials().add(usernamePasswordCredentials);
            publisher.setCredentialsId("username-pass");
            publisher.setResultOnUploadFailure("FAILURE");
            project.getPublishersList().add(publisher);

            FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

            jenkins.assertLogContains("TS-ERROR: Response - 403 Forbidden", build);
        });
    }

    @Test
    public void testSuccessfulUpload() throws Throwable {
        extension.then(jenkins -> {
            try (MockWebServer teamscale = startServer(200)) {
                FreeStyleProject project = createUploadProject(jenkins, urlOf(teamscale));

                FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);

                jenkins.assertLogContains("TS-INFO: Response - 200", build);
                assertEquals(1, teamscale.getRequestCount(), "number of requests received by Teamscale");
                RecordedRequest request = teamscale.takeRequest();
                assertEquals("POST", request.getMethod());
                HttpUrl url = request.getUrl();
                assertEquals(Constants.uploadPath, url.encodedPath());
                assertEquals(Constants.reportFormatId, url.queryParameter("format"));
                assertEquals(Constants.revision, url.queryParameter("revision"));
                assertEquals(Constants.repository, url.queryParameter("repository"));
                assertEquals(Constants.partition, url.queryParameter("partition"));
                assertEquals(Constants.uploadMessage, url.queryParameter("message"));
                String expectedAuthorization = "Basic "
                        + Base64.getEncoder()
                                .encodeToString((Constants.username + ":" + Constants.password)
                                        .getBytes(StandardCharsets.UTF_8));
                assertEquals(expectedAuthorization, request.getHeaders().get("Authorization"));
                assertEquals(
                        "teamscale-upload-jenkins-plugin", request.getHeaders().get("User-Agent"));
                String contentType = request.getHeaders().get("Content-Type");
                assertTrue(contentType != null && contentType.startsWith("multipart/form-data"), contentType);
                String body = request.getBody() == null ? "" : request.getBody().utf8();
                assertTrue(body.contains("name=\"report\"; filename=\"" + Constants.reportFile + "\""), body);
                assertTrue(body.contains(Constants.reportContent), body);
            }
        });
    }

    /**
     * The upload uses a proxy configured via system properties. The plain {@code http(s).proxy*} properties (set together,
     * as in TS-48632) are handled by the JVM's default proxy selector, the {@code teamscale.}-prefixed ones by
     * teamscale-client. An empty {@code http.nonProxyHosts} disables the default exclusion of localhost, so that the
     * request must go through the proxy.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http.,https.", "teamscale.http.", "teamscale.https."})
    public void testUploadUsesProxy(String proxyPropertyPrefixes) throws Throwable {
        extension.then(jenkins -> {
            try (MockWebServer teamscale = startServer(200);
                    MockWebServer proxy = startServer(503)) {
                setProxySystemProperties(proxyPropertyPrefixes, proxy.getPort(), "");
                try {
                    FreeStyleProject project = createUploadProject(jenkins, urlOf(teamscale));

                    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);

                    jenkins.assertLogContains("TS-ERROR: Response - 503", build);
                    assertEquals(1, proxy.getRequestCount(), "number of requests received by the proxy");
                    assertEquals(
                            Constants.uploadPath, proxy.takeRequest().getUrl().encodedPath());
                    assertEquals(0, teamscale.getRequestCount(), "number of requests received by Teamscale");
                } finally {
                    clearProxySystemProperties(proxyPropertyPrefixes);
                }
            }
        });
    }

    /**
     * Regression test for TS-48632: hosts matching {@code http.nonProxyHosts} must be contacted directly, even if a
     * proxy is configured. teamscale-client up to version 34 sent every request through the proxy.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http.,https.", "teamscale.http.", "teamscale.https."})
    public void testUploadBypassesProxyForNonProxyHosts(String proxyPropertyPrefixes) throws Throwable {
        extension.then(jenkins -> {
            try (MockWebServer teamscale = startServer(200);
                    MockWebServer proxy = startServer(503)) {
                setProxySystemProperties(proxyPropertyPrefixes, proxy.getPort(), "*.mycompany.com|localhost");
                try {
                    FreeStyleProject project = createUploadProject(jenkins, urlOf(teamscale));

                    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);

                    jenkins.assertLogContains("TS-INFO: Response - 200", build);
                    assertEquals(1, teamscale.getRequestCount(), "number of requests received by Teamscale");
                    assertEquals(0, proxy.getRequestCount(), "number of requests received by the proxy");
                } finally {
                    clearProxySystemProperties(proxyPropertyPrefixes);
                }
            }
        });
    }

    /**
     * Proxy credentials are configured via {@code teamscale.http(s).proxyUser}/{@code teamscale.http(s).proxyPassword}
     * and sent to a proxy that asks for them. The fake proxy answers like Teamscale once it gets the right credentials.
     * The plain {@code http(s).proxyUser}/{@code http(s).proxyPassword} properties are ignored by the JVM and thus not
     * supported.
     */
    @ParameterizedTest
    @ValueSource(strings = {"teamscale.http.", "teamscale.https."})
    public void testUploadAuthenticatesAtProxy(String proxyPropertyPrefix) throws Throwable {
        extension.then(jenkins -> {
            String expectedCredentials = "Basic "
                    + Base64.getEncoder().encodeToString("proxy-user:proxy-password".getBytes(StandardCharsets.UTF_8));
            try (MockWebServer teamscale = startServer(200);
                    MockWebServer proxy = startServer(request ->
                            expectedCredentials.equals(request.getHeaders().get("Proxy-Authorization"))
                                    ? new MockResponse.Builder().code(200).build()
                                    : new MockResponse.Builder()
                                            .code(407)
                                            .addHeader("Proxy-Authenticate", "Basic realm=\"proxy\"")
                                            .build())) {
                setProxySystemProperties(proxyPropertyPrefix, proxy.getPort(), "");
                System.setProperty(proxyPropertyPrefix + "proxyUser", "proxy-user");
                System.setProperty(proxyPropertyPrefix + "proxyPassword", "proxy-password");
                try {
                    FreeStyleProject project = createUploadProject(jenkins, urlOf(teamscale));

                    FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);

                    jenkins.assertLogContains("TS-INFO: Response - 200", build);
                    RecordedRequest lastRequest = null;
                    for (int i = proxy.getRequestCount(); i > 0; i--) {
                        lastRequest = proxy.takeRequest();
                    }
                    assertTrue(lastRequest != null, "the proxy received no request");
                    assertEquals(
                            expectedCredentials,
                            lastRequest.getHeaders().get("Proxy-Authorization"),
                            "credentials of the last request to the proxy");
                    assertEquals(Constants.uploadPath, lastRequest.getUrl().encodedPath());
                    assertEquals(0, teamscale.getRequestCount(), "number of requests received by Teamscale");
                } finally {
                    clearProxySystemProperties(proxyPropertyPrefix);
                }
            }
        });
    }

    /** Creates a project that uploads a single report to the given URL and fails the build if the upload fails. */
    private static FreeStyleProject createUploadProject(JenkinsRule jenkins, String url) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new SingleFileSCM(Constants.reportFile, Constants.reportContent));
        TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(
                url,
                "teamscale_id",
                Constants.teamscaleProject,
                Constants.partition,
                Constants.uploadMessage,
                Constants.fileFormat,
                Constants.reportFormatId,
                Constants.revision);
        publisher.setRepository(Constants.repository);
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL,
                        "username-pass",
                        "Username / Password credential for testing",
                        Constants.username,
                        Constants.password));
        publisher.setCredentialsId("username-pass");
        publisher.setResultOnUploadFailure("FAILURE");
        project.getPublishersList().add(publisher);
        return project;
    }

    /** Starts a server on localhost that answers every request with the given status code. */
    private static MockWebServer startServer(int responseCode) throws IOException {
        return startServer(
                request -> new MockResponse.Builder().code(responseCode).build());
    }

    /**
     * Starts a server on localhost that answers every request with the response the given function returns for it. Used
     * both as fake Teamscale and as fake HTTP proxy, which receives the requests with absolute URLs.
     */
    private static MockWebServer startServer(Function<RecordedRequest, MockResponse> responder) throws IOException {
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return responder.apply(request);
            }
        });
        server.start();
        return server;
    }

    /** The URL under which the server is reachable via the host name {@code localhost}. */
    private static String urlOf(MockWebServer server) {
        return "http://localhost:" + server.getPort();
    }

    /**
     * Sets the proxy host and port for each of the given comma-separated property prefixes (e.g. {@code http.,https.})
     * and {@code http.nonProxyHosts}.
     */
    private static void setProxySystemProperties(String proxyPropertyPrefixes, int proxyPort, String nonProxyHosts) {
        for (String prefix : proxyPropertyPrefixes.split(",")) {
            System.setProperty(prefix + "proxyHost", "localhost");
            System.setProperty(prefix + "proxyPort", String.valueOf(proxyPort));
        }
        System.setProperty("http.nonProxyHosts", nonProxyHosts);
    }

    private static void clearProxySystemProperties(String proxyPropertyPrefixes) {
        for (String prefix : proxyPropertyPrefixes.split(",")) {
            System.clearProperty(prefix + "proxyHost");
            System.clearProperty(prefix + "proxyPort");
            System.clearProperty(prefix + "proxyUser");
            System.clearProperty(prefix + "proxyPassword");
        }
        System.clearProperty("http.nonProxyHosts");
    }
}
