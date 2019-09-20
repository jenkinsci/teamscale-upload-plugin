package eu.cqse.teamscale.jenkins.upload;

import eu.cqse.teamscale.client.EReportFormat;
import eu.cqse.teamscale.client.ITeamscaleService;
import eu.cqse.teamscale.client.TeamscaleServer;
import eu.cqse.teamscale.client.TeamscaleServiceGenerator;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class TeamscaleUploadBuilder extends Builder implements SimpleBuildStep {

    /** Api for uploaing files to teamscale. */
    private ITeamscaleService api;

    /** Stored teamscale server settings. */
    private final TeamscaleServer server;

    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String userName, String ideKey, String teamscaleProject, String partition, String uploadMessage) {
        server = new TeamscaleServer(url, userName, ideKey, teamscaleProject, partition, uploadMessage);
    }

    public String getUrl() {
        return server.url;
    }

    public String getIdeKey() {
        return server.userAccessToken;
    }

    public String getUserName() {
        return server.userName;
    }

    public String getTeamscaleProject() {
        return server.project;
    }

    public String getPartition() {
        return server.partition;
    }

    public String getUploadMessage() {
        return server.message;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        api = TeamscaleServiceGenerator.createServiceWithRequestLogging(
                ITeamscaleService.class,
                HttpUrl.parse(server.url),
                server.userName,
                server.userAccessToken,
                listener.getLogger()
        );


        tryUploading("TEst\n1-1");
    }

    /** Performs the upload and returns <code>true</code> if successful. */
    private void tryUploading(String data) {
        try {
            api.uploadReport(
                    server.project,
                    server.partition,
                    EReportFormat.SIMPLE,
                    server.message,
                    RequestBody.create(MultipartBody.FORM, data)
            );
        } catch (IOException ignored) {
        }
    }


    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.TeamscaleBuilder_DescriptorImpl_errors_requiredField());
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.TeamscaleBuilder_DescriptorImpl_DisplayName();
        }

    }

}
