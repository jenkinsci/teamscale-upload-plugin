package eu.cqse.teamscale.jenkins.upload;

import static org.junit.jupiter.api.Assertions.*;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlSelect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;

class TeamscaleUploadPluginConfigurationTest {

    @RegisterExtension
    private final RealJenkinsExtension extension = new RealJenkinsExtension();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */
    @Test
    void uiAndStorage() throws Throwable {
        extension.then(jenkins -> {
            assertEquals("FAILURE", TeamscaleUploadPluginConfiguration.get().getResultNoReports(), "not set initially");
            assertEquals(
                    "UNSTABLE",
                    TeamscaleUploadPluginConfiguration.get().getResultOnUploadFailure(),
                    "not set initially");
            try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
                HtmlForm config = client.goTo("configure").getFormByName("config");
                HtmlSelect textbox = config.getSelectByName("_.resultNoReports");
                textbox.setSelectedAttribute("UNSTABLE", true);
                textbox = config.getSelectByName("_.resultOnUploadFailure");
                textbox.setSelectedAttribute("FAILURE", true);
                jenkins.submit(config);
                assertEquals(
                        "UNSTABLE",
                        TeamscaleUploadPluginConfiguration.get().getResultNoReports(),
                        "global config page let us edit it");
                assertEquals(
                        "FAILURE",
                        TeamscaleUploadPluginConfiguration.get().getResultOnUploadFailure(),
                        "global config page let us edit it");
            }
        });
        extension.then(jenkins -> {
            assertEquals(
                    "UNSTABLE",
                    TeamscaleUploadPluginConfiguration.get().getResultNoReports(),
                    "still there after restart of Jenkins");
            assertEquals(
                    "FAILURE",
                    TeamscaleUploadPluginConfiguration.get().getResultOnUploadFailure(),
                    "still there after restart of Jenkins");
        });
    }
}
