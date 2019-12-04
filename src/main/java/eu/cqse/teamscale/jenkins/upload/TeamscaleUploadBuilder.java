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
     * TODO (ToP) Typo: capital 'T' for Teamscale
     * Api for uploading files to teamscale.
     */
    private ITeamscaleService api;

    public static final String ERROR = "TS-ERROR: ";
    public static final String WARNING = "TS-WARNING: ";
    public static final String INFO = "TS-INFO: ";

    private static final String EXEC_FOLDER = "exec";

    // TODO (ToP) I'd remove the newlines between the fields to make the class less spaced out and thus easier to look over.
    private String url;

    private String userName;

    private String ideKey;

    private String teamscaleProject;

    private String partition;

    private String uploadMessage;

    private String files; //TODO (ToP) Please rename to something indicating that this is the pattern specified by the user, not the actual files

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

    // TODO (ToP) I left some comments about extracting a method below. My general idea would be to have this method as a high level overview, what the plugin is doing when it's executed, i.e.
    //  - Calculating the file name for timestamp tool
    //  - Copying the tool to the workspace
    //  - Getting a list of files to upload defined by the ant pattern provided by the user
    //  - Get information which is needed to upload files to Teamscale (i.e. branch and timestamp). Maybe this could be part of the next bullet point?
    //  - Uploading the files to Teamscale
    //  All of those bullet points can be implemented as one method each, which then are just called in the perform method.
    //  Everything below is one abstraction level lower and does not matter to the reader of the code in this place.
    //  In case you have any questions or want to discuss this, feel free to contact me personally ;)
    @Override
    // TODO (ToP) IntelliJ warns me about missing @Nonnull annotations for the parameters. Wouldn't harm to add those I think :)
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        // TODO (ToP) Please rename to something more indicative which exec is referred to, e.g. "timestampToolExecutableName". Also, this part of the name can be static.
        String execName = "teamscale-timestamp";
        // TODO (ToP) I think we want to check for "is Windows", not "is not Linux". We also wouldn't need the ".exe" file extension on Mac.
        if (!getOperatingSystem().toLowerCase().contains("linux")) {
            execName += ".exe";
        }
        // TODO (ToP) Please extract the code above to a separate method, as this level of detail is not adequate for the `perform` method
        //  `String timestampToolName = getPlatformSpecificTimestampToolName();` would be more readable on this level of abstraction.
        //  Although I do like the idea of the `getOperatingSystem()` method, you could "replace" the it with the newly created one,
        //  i.e. do `return System.getProperty("os.name");` in `getPlatformSpecificTimestampToolName` directly, since the abstraction level would be fitting there.


        // TODO (ToP) Same as for the part above, please extract copying to a new method
        File destination = new File(workspace.toURI().getPath() + File.separator + execName);
        destination.setExecutable(true); //Currently ignored. // TODO (ToP) Do you mean the result with this comment? If so, explain why it is ignored or remove the comment.

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

        // TODO (ToP) Any reason why this is not a static method in a Utils file or helper method in this class?
        DirScanner ds = new DirScanner(getFiles());
        List<File> files = ds.list(new File(workspace.toURI().getPath()));

        // TODO (ToP) If you return an empty list instead of null in case no files are found, you can avoid the null check and replace the if with the for loop inside the else block
        //  The log would be missing then of course. In case you want to keep it, I'd still return an empty list instead of null as it's a more robust default.
        //  Also: `files.size()` cannot be <0 :P
        if (files == null || files.size() <= 0) {
            listener.getLogger().println(ERROR + "No files found with pattern " + getFiles());
        } else {
            // TODO (ToP) Could also be extracted to a helper method (e.g. `uploadFilesToTeamscale`)
            String branchAndTimeStamp = getBranchAndTimeStamp(workspace, execName);
            String branchName = branchAndTimeStamp.substring(0, branchAndTimeStamp.indexOf(':'));
            String timeStamp = branchAndTimeStamp.substring(branchAndTimeStamp.indexOf(':') + 1);

            listener.getLogger().println(INFO + "Branch: " + branchName);
            listener.getLogger().println(INFO + "Timestamp: " + timeStamp);
            for (File file : files) {
                String fileContentAsString  = getFileContentAsString(workspace, file); // TODO (ToP) You could use `FileUtils.readFileToString` here ;)
                uploadReport(fileContentAsString, branchName, timeStamp);
            }
        }
    }

    @Nonnull
    private String getBranchAndTimeStamp(FilePath workspace, String execName) throws IOException, InterruptedException {
        // TODO (ToP) please don't use one or two letter variable names, it's easier to read `process` instead of `p`, especially reading for the first time
        //  Same for `is` and `n`
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
     * @param branchname for external upload. // TODO (ToP) typo: branchName
     * @param timestamp  for external upload.
     */
    private void uploadReport(String data, String branchname, String timestamp) { // TODO (ToP) typo: branchName
        Call<ResponseBody> apiRequest = api.uploadExternalReport(
                getTeamscaleProject(),
                "SIMPLE", // TODO (ToP) Use the setting specified by the user instead of the hardcoded value :P
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
            // TODO (ToP) I think we need validation for all form fields here ;)
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
