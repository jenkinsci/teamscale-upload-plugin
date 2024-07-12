package eu.cqse.teamscale.jenkins.upload;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

@RunWith(Parameterized.class)
public class TeamscaleUploadBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String url = "localhost:8100";
    private final String teamscaleProject = "jenkinsplugin";
    private final String partition = "simple";
    private final String uploadMessage = "Uploaded simple coverage";
    private final String fileFormat = "**/*.simple";
    private final String reportFormatId = "SIMPLE";

    @Parameters
    public static Object[] data() {
        return new Object[] { null, "a" };
    }

    @Parameter
    public String repository;

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new TeamscaleUploadBuilder(url, "teamscale_id", teamscaleProject, partition, repository, uploadMessage, fileFormat, reportFormatId, ""));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new  TeamscaleUploadBuilder(url, "teamscale_id", teamscaleProject, partition, repository, uploadMessage, fileFormat, reportFormatId, ""), project.getPublishersList().get(0));
    }

    @Test
    public void testPipelineWithoutCredentials() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new SingleFileSCM("test.simple", "RunExec.java\n8-10"));
        TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(url, "teamscale_id", teamscaleProject, partition, repository, uploadMessage, fileFormat, reportFormatId, "");
        project.getPublishersList().add(publisher);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("credentials are null", build);
    }

}