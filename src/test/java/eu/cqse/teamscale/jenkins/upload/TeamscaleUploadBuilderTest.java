package eu.cqse.teamscale.jenkins.upload;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
                    "TS-ERROR: Failed to upload reports to Teamscale: Failed to connect to localhost/127.0.0.1:8100",
                    build);
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
}
