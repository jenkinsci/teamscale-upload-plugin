package eu.cqse.teamscale.jenkins.upload;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.Secret;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import retrofit2.Call;

/**
 * The Teamscale Jenkins plugin.
 * The inheritance from Notifier marks is as a post build action plugin.
 */
public class TeamscaleUploadBuilder extends Notifier implements SimpleBuildStep {

    /**
     * Api for uploading files to Teamscale.
     */
    private ITeamscaleService api;

    /** For printing errors to jenkins console. */
    public static final String ERROR = "TS-ERROR: ";

    /** For printing warnings to jenkins console. */
    public static final String WARNING = "TS-WARNING: ";

    /** For printing info to jenkins console. */
    public static final String INFO = "TS-INFO: ";

    private static final String EXEC_FOLDER = "exec";

    private String url;
    private String userName;
    private Secret ideKey;
    private String teamscaleProject;
    private String partition;
    private String uploadMessage;
    private String antPatternForFileScan;
    private String reportFormatId;

    /**
     * Automatic data binding on save of the plugin configuration in jenkins.
     * @param url to save.
     * @param userName to save.
     * @param ideKey to save.
     * @param teamscaleProject to save.
     * @param partition to save.
     * @param uploadMessage to save.
     * @param antPatternForFileScan to save.
     * @param reportFormatId to save.
     */
    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String userName, Secret ideKey, String teamscaleProject, String partition, String uploadMessage, String antPatternForFileScan, String reportFormatId) {
        this.url = url;
        this.userName = userName;
        this.ideKey = ideKey;
        this.teamscaleProject = teamscaleProject;
        this.partition = partition;
        this.uploadMessage = uploadMessage;
        this.antPatternForFileScan = antPatternForFileScan;
        this.reportFormatId = reportFormatId;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public Secret getIdeKey() {
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

    public String getAntPatternForFileScan() {
        return antPatternForFileScan;
    }

    public String getReportFormatId() {
        return reportFormatId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        String timestampToolExecutableName = getPlatformSpecificTimestampToolName();

        copyToolToWorkspace(workspace, listener.getLogger(), timestampToolExecutableName);

        api = TeamscaleServiceGenerator.createService(
                ITeamscaleService.class,
                HttpUrl.parse(getUrl()),
                getUserName(),
                getIdeKey().getPlainText(),
                new JenkinsConsoleInterceptor(listener.getLogger())
        );

        uploadFilesToTeamscale(workspace, listener.getLogger(), timestampToolExecutableName);
    }

    /**
     * Upload test results specified by ant-pattern to the teamscale server.
     *
     * @param workspace of jenkins project.
     * @param printStream writing logging output to.
     * @param timestampToolExecutableName name of the timestamp executable.
     * @throws IOException access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    private void uploadFilesToTeamscale(FilePath workspace, @Nonnull PrintStream printStream, String timestampToolExecutableName) throws IOException, InterruptedException {
        List<File> files = TeamscaleUploadUtilities.getFiles(new File(workspace.toURI().getPath()), getAntPatternForFileScan());

        if (files.isEmpty()) {
            printStream.println(ERROR + "No files found with pattern " + getAntPatternForFileScan());
        } else {
            String branchAndTimeStamp = getBranchAndTimeStamp(workspace, timestampToolExecutableName);
            String branchName = branchAndTimeStamp.substring(0, branchAndTimeStamp.indexOf(':'));
            String timeStamp = branchAndTimeStamp.substring(branchAndTimeStamp.indexOf(':') + 1);

            printStream.println(INFO + "Branch: " + branchName);
            printStream.println(INFO + "Timestamp: " + timeStamp);
            for (File file : files) {
                File currentFile = new File(workspace.toURI().getPath() + File.separator + file.toString());
                String fileContentAsString  =  FileUtils.readFileToString(currentFile, "UTF-8");
                uploadReport(fileContentAsString, branchName, timeStamp);
            }
        }
    }

    /**
     * Copy timestamp tool for version control system to the workspace of the project.
     *
     * @param workspace of jenkins project.
     * @param printStream writing logging output to.
     * @param timestampToolExecutableName name of the timestamp executable.
     * @throws IOException access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    private void copyToolToWorkspace(FilePath workspace, @Nonnull PrintStream printStream, String timestampToolExecutableName) throws IOException, InterruptedException {
        File destination = new File(workspace.toURI().getPath() + File.separator + timestampToolExecutableName);
        destination.setExecutable(true);

        if (!destination.exists()) {
            try {
                InputStream sourceStream = this.getClass().getClassLoader().getResourceAsStream(EXEC_FOLDER + "/" + timestampToolExecutableName);
                FileUtils.copyInputStreamToFile(sourceStream, destination);
                printStream.println(INFO + "Copied timestamp");
            } catch (IOException e) {
                printStream.println(e);
            }
        } else {
            printStream.println(INFO + "Did not copy timestamp, it already exists!");
        }
    }

    /**
     * Retrieve branch and timestamp of version control system belonging to the workspace and the project.
     *
     * @param workspace of jenkins project.
     * @param timestampExecutableName name of the timestamp executable.
     * @return branch and timestamp ':' separated
     * @throws IOException access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    @Nonnull
    private String getBranchAndTimeStamp(FilePath workspace, String timestampExecutableName) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(workspace.toURI().getPath() + File.separator + timestampExecutableName, null, new File(workspace.toURI()));

        InputStream inputStream = process.getInputStream();
        StringBuilder build = new StringBuilder();
        int currentRead = inputStream.read();
        while (currentRead != -1 && currentRead != '\n') {
            build.append((char) currentRead);
            currentRead = inputStream.read();
        }
        return build.toString();
    }

    /**
     * Performs an upload of an external report.
     *
     * @param data       to upload.
     * @param branchName for external upload.
     * @param timestamp  for external upload.
     */
    private void uploadReport(String data, String branchName, String timestamp) {
        Call<ResponseBody> apiRequest = api.uploadExternalReport(
                getTeamscaleProject(),
                getReportFormatId(),
                new CommitDescriptor(branchName, timestamp),
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

    /**
     * Determine which timestamp tool to use for MAC, Linux or Windows.
     * @return timestamp-tool name.
     */
    private String getPlatformSpecificTimestampToolName(){
        String timestampToolExecutableName = "teamscale-timestamp";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            timestampToolExecutableName += ".exe";
        }
        return timestampToolExecutableName;
    }


    /**
     * Description/Hint provided if user does not fill out the plugin fields correctly.
     */
    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckUserName(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckIdeKey(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckTeamscaleProject(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckPartition(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckUploadMessage(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckAntPatternForFileScan(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        public FormValidation doCheckReportFormatId(@QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        /**
         * Checks the value of an input field of the plugin.
         * @param value to check.
         * @return ok or not okay.
         */
        private FormValidation getFormValidation(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(Messages.TeamscaleBuilder_DescriptorImpl_errors_requiredField());
            }
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
