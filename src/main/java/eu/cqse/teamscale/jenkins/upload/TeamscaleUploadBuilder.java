package eu.cqse.teamscale.jenkins.upload;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.tasks.BuildStepDescriptor;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.*;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class TeamscaleUploadBuilder extends Notifier implements SimpleBuildStep {

    /** Api for uploaing files to teamscale. */
    private ITeamscaleService api;

    /** Stored teamscale server settings. */
    private TeamscaleServer server;

    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String userName, String ideKey, String teamscaleProject, String partition, String uploadMessage) {
        server = new TeamscaleServer();
        server.url = HttpUrl.parse(url);
        server.userName = userName;
        server.userAccessToken = ideKey;
        server.project = teamscaleProject;
        server.partition = partition;
        server.message = uploadMessage;
    }

    public String getUrl() {
        return server.url.toString();
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
        listener.getLogger().println(this.getClass().getClassLoader().getResource("exec/teamscale-timestamp"));

        Process p = Runtime.getRuntime().exec(this.getClass().getClassLoader().getResource("exec/teamscale-timestamp").getPath());
        File src = new File(this.getClass().getClassLoader().getResource("exec/teamscale-timestamp").getPath());
        File dst = new File(workspace.toURI());
        try{
            FileUtils.copyToDirectory(src, dst);
        }catch(IOException e){
            listener.getLogger().println(e);
        }
        InputStream is = p.getInputStream();
        StringBuilder build = new StringBuilder();
        int n = is.read();
        while(n != -1 && n != '\n')
        {
            build.append((char) n);
            n = is.read();
        }
        listener.getLogger().println("Timestamp: " + build.toString());

        api = TeamscaleServiceGenerator.createService(
                ITeamscaleService.class,
                HttpUrl.parse(getUrl()),
                server.userName,
                server.userAccessToken,
                new JenkinsConsoleInterceptor(listener.getLogger())
        );

        File file = new File(workspace.toString() + "/coverage.simple");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String currentRead = br.readLine();
        StringBuilder result = new StringBuilder();
        while(currentRead != null){
            result.append(currentRead);
            result.append("\r\n");
            currentRead = br.readLine();
        }

        uploadReport(result.toString());
        listener.getLogger().println("Workspace " + workspace.toString());
        listener.getLogger().println("File " + result.toString());
    }

    /** Performs the upload and returns <code>true</code> if successful. */
    private void uploadReport(String data) {
            api.uploadExternalReport(
                    getTeamscaleProject(),
                    "SIMPLE",
                    new CommitDescriptor("", ""),
                    true,
                    false,
                    getPartition(),
                    getUploadMessage(),
                    RequestBody.create(data, MultipartBody.FORM)
            );
    }



    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
