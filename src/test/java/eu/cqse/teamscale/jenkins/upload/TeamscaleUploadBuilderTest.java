package eu.cqse.teamscale.jenkins.upload;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TeamscaleUploadBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String url = "localhost:8100";
    private final String user = "admin";
    private final String ideKey = "WeAil7JtESNPnuopxmTCotGxmpvKqXFf";
    private final String teamscaleProject = "jenkinsplugin";
    private final String partition = "simple";
    private final String uploadMessage = "Uploaded simple coverage";
    private final String fileFormat = "**/*.simple";
    private final String reportFormatId = "SIMPLE";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(new TeamscaleUploadBuilder(url, user, ideKey, teamscaleProject, partition, uploadMessage, fileFormat, reportFormatId));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new TeamscaleUploadBuilder(url, user, ideKey, teamscaleProject, partition, uploadMessage, fileFormat, reportFormatId), project.getPublishersList().get(0));
    }

//    @Test
//    public void testConfigRoundtripFrench() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        TeamscaleUploadBuilder publisher = new TeamscaleUploadBuilder(url, user, ideKey, teamscaleProject, partition, uploadMessage);
//        project.getPublishersList().add(publisher);
//        project = jenkins.configRoundtrip(project);
//
//        TeamscaleUploadBuilder lhs = new TeamscaleUploadBuilder(url, user, ideKey, teamscaleProject, partition, "Nope");
//        jenkins.assertEqualDataBoundBeans(lhs, project.getPublishersList().get(0));
//    }

//    @Test
//    public void testConfigRoundtripFrench() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        TeamscaleUploadBuilder builder = new TeamscaleUploadBuilder(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//        project = jenkins.configRoundtrip(project);
//
//        TeamscaleUploadBuilder lhs = new TeamscaleUploadBuilder(name);
//        lhs.setUseFrench(true);
//        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
//    }
//
//    @Test
//    public void testBuild() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        TeamscaleUploadBuilder builder = new TeamscaleUploadBuilder(name);
//        project.getBuildersList().add(builder);
//
//        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
//        jenkins.assertLogContains("Hello, " + name, build);
//    }
//
//    @Test
//    public void testBuildFrench() throws Exception {
//
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        TeamscaleUploadBuilder builder = new TeamscaleUploadBuilder(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//
//        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
//        jenkins.assertLogContains("Bonjour, " + name, build);
//    }
//
//    @Test
//    public void testScriptedPipeline() throws Exception {
//        String agentLabel = "my-agent";
//        jenkins.createOnlineSlave(Label.get(agentLabel));
//        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
//        String pipelineScript
//                = "node {\n"
//                + "  greet '" + name + "'\n"
//                + "}";
//        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
//        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
//        String expectedString = "Hello, " + name + "!";
//        jenkins.assertLogContains(expectedString, completedBuild);
//    }

}