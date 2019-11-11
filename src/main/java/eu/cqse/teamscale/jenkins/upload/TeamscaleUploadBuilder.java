package eu.cqse.teamscale.jenkins.upload;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.*;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.tasks.BuildStepDescriptor;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import retrofit2.Call;

public class TeamscaleUploadBuilder extends Notifier implements SimpleBuildStep {

    /**
     * Api for uploading files to teamscale.
     */
    private ITeamscaleService api;

    public static final String ERROR = "TS-ERROR: ";
    public static final String WARNING = "TS-WARNING: ";
    public static final String INFO = "TS-INFO: ";

    private static final String EXEC_FOLDER = "exec";

    private String url;

    private String userName;

    private String ideKey;

    private String teamscaleProject;

    private String partition;

    private String uploadMessage;

    private String files;

    private String reportFormatId;

    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String userName, String ideKey, String teamscaleProject, String partition, String uploadMessage, String files, String reportFormatId) {
        this.url = url;
        this.userName = userName;
        this.ideKey = ideKey;
        this.teamscaleProject = teamscaleProject;
        this.partition = partition;
        this.uploadMessage = uploadMessage;
        this.files = files;
        this.reportFormatId = reportFormatId;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getIdeKey() {
        return ideKey;
    }

    public String getTeamscaleProject() {
        return teamscaleProject;
    }

    public String getPartition() {
        return partition;
    }

    public String getUploadMessage() {
        return uploadMessage;
    }

    public String getFiles() {
        return files;
    }

    public String getReportFormatId() {
        return reportFormatId;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        String execName = "teamscale-timestamp";
        if (!getOperatingSystem().toLowerCase().contains("linux")) {
            execName += ".exe";
        }

        File destination = new File(workspace.toURI().getPath() + File.separator + execName);
        destination.setExecutable(true); //Currently ignored.

        if (!destination.exists()) {
            try {
                InputStream sourceStream = this.getClass().getClassLoader().getResourceAsStream(EXEC_FOLDER + "/" + execName);
                FileUtils.copyInputStreamToFile(sourceStream, destination);
                listener.getLogger().println(INFO + "Copied file");
            } catch (IOException e) {
                listener.getLogger().println(e);
            }
        } else {
            listener.getLogger().println(INFO + "Did not copy file. Timestamp already exists!");
        }

        api = TeamscaleServiceGenerator.createService(
                ITeamscaleService.class,
                HttpUrl.parse(getUrl()),
                getUserName(),
                getIdeKey(),
                new JenkinsConsoleInterceptor(listener.getLogger())
        );

        DirScanner ds = new DirScanner(getFiles());
        List<File> files = ds.list(new File(workspace.toURI().getPath()));

        if (files == null || files.size() <= 0) {
            listener.getLogger().println(ERROR + "No files found with pattern " + getFiles());
        } else {
            String branchAndTimeStamp = getBranchAndTimeStamp(workspace, execName);
            String branchName = branchAndTimeStamp.substring(0, branchAndTimeStamp.indexOf(':'));
            String timeStamp = branchAndTimeStamp.substring(branchAndTimeStamp.indexOf(':') + 1);

            listener.getLogger().println(INFO + "Branch: " + branchName);
            listener.getLogger().println(INFO + "Timestamp: " + timeStamp);
            for (File file : files) {
                String fileContentAsString  = getFileContentAsString(workspace, file);
                uploadReport(fileContentAsString, branchName, timeStamp);
            }
        }
    }

    @Nonnull
    private String getBranchAndTimeStamp(FilePath workspace, String execName) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(workspace.toURI().getPath() + File.separator + execName);

        InputStream is = p.getInputStream();
        StringBuilder build = new StringBuilder();
        int n = is.read();
        while (n != -1 && n != '\n') {
            build.append((char) n);
            n = is.read();
        }
        return build.toString();
    }

    @Nonnull
    private String getFileContentAsString(FilePath workspace, File file) throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(workspace.toURI().getPath() + File.separator + file), StandardCharsets.UTF_8));
        String currentRead = br.readLine();
        StringBuilder result = new StringBuilder();
        while (currentRead != null) {
            result.append(currentRead);
            result.append("\r\n");
            currentRead = br.readLine();
        }
        br.close();
        return result.toString();
    }


    /**
     * Performs an upload of an external report.
     *
     * @param data       to upload.
     * @param branchname for external upload.
     * @param timestamp  for external upload.
     */
    private void uploadReport(String data, String branchname, String timestamp) {
        Call<ResponseBody> apiRequest = api.uploadExternalReport(
                getTeamscaleProject(),
                "SIMPLE",
                new CommitDescriptor(branchname, timestamp),
                true,
                false,
                getPartition(),
                getUploadMessage(),
                RequestBody.create(data, MultipartBody.FORM)
        );

        try {
            apiRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getOperatingSystem() {
        return System.getProperty("os.name");
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
