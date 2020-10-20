package eu.cqse.teamscale.jenkins.upload;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.teamscale.client.CommitDescriptor;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import retrofit2.Call;

/**
 * The Teamscale Jenkins plugin.
 * The inheritance from Notifier marks is as a post build action plugin.
 */
public class TeamscaleUploadBuilder extends Notifier implements SimpleBuildStep {

    /**
     * Matcher for populating the credentials dropdown
     */
    private static final CredentialsMatcher MATCHER = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

    /**
     * Api for uploading files to Teamscale.
     */
    private ITeamscaleService api;

    /**
     * For printing errors to jenkins console.
     */
    public static final String ERROR = "TS-ERROR: ";

    /**
     * For printing warnings to jenkins console.
     */
    public static final String WARNING = "TS-WARNING: ";

    /**
     * For printing info to jenkins console.
     */
    public static final String INFO = "TS-INFO: ";

    private static final String EXEC_FOLDER = "exec";

    private final String url;
    private final String teamscaleProject;
    private final String partition;
    private final String uploadMessage;
    private final String antPatternForFileScan;
    private final String reportFormatId;


    private String credentialsId;

    /**
     * Automatic data binding on save of the plugin configuration in jenkins.
     *
     * @param url                   to save.
     * @param teamscaleProject      to save.
     * @param partition             to save.
     * @param uploadMessage         to save.
     * @param antPatternForFileScan to save.
     * @param reportFormatId        to save.
     */
    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String credentialsId, String teamscaleProject, String partition, String uploadMessage, String antPatternForFileScan, String reportFormatId) {
        this.url = url;
        this.teamscaleProject = teamscaleProject;
        this.partition = partition;
        this.uploadMessage = uploadMessage;
        this.antPatternForFileScan = antPatternForFileScan;
        this.reportFormatId = reportFormatId;
        this.credentialsId = credentialsId;
    }

    public String getUrl() {
        return url;
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

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {


        String timestampToolExecutableName = getPlatformSpecificTimestampToolName();

        StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
                credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.fromUri(getUrl()).build());

        if (credential == null) {
            listener.getLogger().println(ERROR + "credentials are null");
            return;
        }

        copyToolToWorkspace(workspace, listener.getLogger(), timestampToolExecutableName);

        HttpUrl url = HttpUrl.parse(getUrl());
        if (url == null) {
            return;
        }

        api = TeamscaleServiceGenerator.createService(
                ITeamscaleService.class,
                url,
                credential.getUsername(),
                credential.getPassword().getPlainText(),
                new JenkinsConsoleInterceptor(listener.getLogger())
        );

        String revision = getScmRevision(run.getEnvironment(listener));
        if (revision == null) {
            listener.getLogger().println(ERROR + "Could not find any revision. Currently only GIT and SVN are supported.");
            return;
        }

        List<File> files = getFilesToUpload(workspace);

        if(files.isEmpty()) {
            listener.getLogger().println(INFO + "No files found to upload to Teamscale with pattern \"" + getAntPatternForFileScan() + "\"");
            return;
        }
        uploadFilesToTeamscale(files, revision);
    }

    private List<File> getFilesToUpload(FilePath workspace) throws IOException, InterruptedException{
        List<File> files = TeamscaleUploadUtilities.getFiles(new File(workspace.toURI().getPath()), getAntPatternForFileScan());
        for (File file : files) {
            File currentFile = new File(workspace.toURI().getPath() + File.separator + file.toString()); // replace with file.getName()
        }
        return files;
    }

    /**
     * Upload test results specified by ant-pattern to the teamscale server.
     *
     * @throws IOException          access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    private void uploadFilesToTeamscale(List<File> files, String revision) throws IOException, InterruptedException {
            for (File file : files) {
                String fileContentAsString = FileUtils.readFileToString(new File(file.getPath()), "UTF-8");
                uploadReport(fileContentAsString, revision);
            }
    }

    private String getScmRevision(EnvVars envVars) {
        String gitCommit = envVars.get("GIT_COMMIT");
        if (gitCommit != null) {
            return gitCommit;
        }
        return envVars.get("SVN_REVISION");
    }

    /**
     * Copy timestamp tool for version control system to the workspace of the project.
     *
     * @param workspace                   of jenkins project.
     * @param printStream                 writing logging output to.
     * @param timestampToolExecutableName name of the timestamp executable.
     * @throws IOException          access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    private void copyToolToWorkspace(FilePath workspace, @Nonnull PrintStream printStream, String timestampToolExecutableName) throws IOException, InterruptedException {
        File destination = new File(workspace.toURI().getPath() /*+ File.separator*/ + timestampToolExecutableName);
        destination.setExecutable(true);

        if (!destination.exists()) {
            try {
                InputStream sourceStream = this.getClass().getClassLoader().getResourceAsStream(EXEC_FOLDER + File.separator + timestampToolExecutableName);
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
     * @param workspace               of jenkins project.
     * @param timestampExecutableName name of the timestamp executable.
     * @return branch and timestamp ':' separated
     * @throws IOException          access on timestamp tool not successful.
     * @throws InterruptedException executable thread of timestamp tool was interrupted.
     */
    @Nonnull
    private String getBranchAndTimeStamp(FilePath workspace, String timestampExecutableName) throws IOException, InterruptedException {

        Process process = Runtime.getRuntime().exec(workspace.toURI().getPath() /*+ File.separator*/ + timestampExecutableName, null, new File(workspace.toURI()));

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
     * @param data     to upload.
     * @param revision coverage is applied to.
     */
    private void uploadReport(String data, String revision) {
        Call<ResponseBody> apiRequest = api.uploadExternalReport(
                getTeamscaleProject(),
                getReportFormatId(),
                null,
                revision,
                true,
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
     *
     * @return timestamp-tool name.
     */
    private String getPlatformSpecificTimestampToolName() {
        String timestampToolExecutableName = "teamscale-timestamp";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            timestampToolExecutableName += ".exe";
        }
        return timestampToolExecutableName;
    }


    /**
     * Description/Hint provided if user does not fill out the plugin fields correctly.
     */
    @Symbol("teamscale")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckUrl(@QueryParameter String value)
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
         * Populates the dropdown for the credentials matching {@literal MATCHER}
         *
         * @param project       to look in.
         * @param url           jenkins server url
         * @param credentialsId current populated id
         * @return list of credentials
         */
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item project,
                @QueryParameter String url,
                @QueryParameter String credentialsId
        ) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (project == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!project.hasPermission(Item.EXTENDED_READ)
                        && !project.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            return result
                    .includeMatchingAs(
                            project instanceof Queue.Task ?
                                    Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri(url).build(),
                            MATCHER)
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(
                @AncestorInPath Item item,
                @QueryParameter String url,
                @QueryParameter String value
        ) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (value.startsWith("${") && value.endsWith("}")) {
                return FormValidation.warning("Cannot validate expression based credentials");
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Upload will fail without credentials");
            }
            if (CredentialsProvider.listCredentials(
                    StandardUsernameCredentials.class,
                    item,
                    item instanceof Queue.Task ?
                            Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
                    URIRequirementBuilder.fromUri(url).build(),
                    CredentialsMatchers.withId(value)
            ).isEmpty()) {
                return FormValidation.error("Cannot find currently selected credentials");
            }
            return FormValidation.ok();
        }

        /**
         * Checks the value of an input field of the plugin.
         *
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
