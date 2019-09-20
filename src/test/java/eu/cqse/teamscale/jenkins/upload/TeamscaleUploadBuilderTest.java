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

    final String url = "localhost:8100";
    final String user = "admin";
    final String ideKey = "WeAil7JtESNPnuopxmTCotGxmpvKqXFf";
    final String teamscaleProject = "jenkinsplugin";
    final String partition = "simple";
    final String uploadMessage = "Uploaded simple coverage";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TeamscaleUploadBuilder(url, user, ideKey, teamscaleProject, partition, uploadMessage));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new TeamscaleUploadBuilder(url, user, ideKey, teamscalePorject, partition, uploadMessage), project.getBuildersList().get(0));
    }

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