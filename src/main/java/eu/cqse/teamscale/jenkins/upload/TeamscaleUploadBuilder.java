package eu.cqse.teamscale.jenkins.upload;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.teamscale.client.EReportFormat;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import retrofit2.Call;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final String url;
    private final String teamscaleProject;
    private final String partition;
    private final String uploadMessage;
    private final String includePattern;
    private final String reportFormatId;

    private String revision;

    private String credentialsId;

    /**
     * Automatic data binding on save of the plugin configuration in jenkins.
     *
     * @param url              to save.
     * @param teamscaleProject to save.
     * @param partition        to save.
     * @param uploadMessage    to save.
     * @param includePattern   to save.
     * @param reportFormatId   to save.
     * @param revision         to save. Required in pipeline projects.
     */
    @DataBoundConstructor
    public TeamscaleUploadBuilder(String url, String credentialsId, String teamscaleProject, String partition, String uploadMessage, String includePattern, String reportFormatId, String revision) {
        this.url = url;
        this.teamscaleProject = teamscaleProject;
        this.partition = partition;
        this.uploadMessage = uploadMessage;
        this.includePattern = includePattern;
        this.reportFormatId = reportFormatId;
        this.credentialsId = credentialsId;
        this.revision = revision;
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

    public String getIncludePattern() {
        return includePattern;
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

    public String getRevision() {
        return revision;
    }

    @DataBoundSetter
    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
                credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.fromUri(getUrl()).build());

        if (credential == null) {
            listener.getLogger().println(ERROR + "credentials are null");
            return;
        }

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


        String rev = getScmRevision(run.getEnvironment(listener));
        listener.getLogger().println(INFO + "revision: " + revision);
        if (rev == null) {
            listener.getLogger().println(ERROR + "Could not find any revision. Currently only GIT and SVN are supported.");
            return;
        }


        Map<String, String> reports = workspace.act(new CoverageCollectingFileCallable(new String[]{getIncludePattern()}));

        if (reports.isEmpty()) {
            listener.getLogger().println(INFO + "No files found to upload to Teamscale with pattern \"" + getIncludePattern() + "\"");
            return;
        }
        uploadReports(reports, rev);
    }


    /**
     * Retrieves the SCM revision.
     * Either takes the parameter from the constructor if it matches certain criteria or checks the environment variables for SVN or GIT revisions.
     *
     * @param envVars environment variables during run time.
     * @return null or revision
     */
    private String getScmRevision(EnvVars envVars) {
        if (revision != null) {
            Pattern p = Pattern.compile("^((([a-f]|[0-9])+)|([0-9])+)$");
            Matcher m = p.matcher(revision);
            if (m.matches()) {
                return revision;
            }
        }
        String gitCommit = envVars.get("GIT_COMMIT");
        if (gitCommit != null) {
            return gitCommit;
        }
        return envVars.get("SVN_REVISION");
    }

    private void uploadReports(Map<String, String> reports, String revision) {
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (String file : reports.keySet()) {
            parts.add(MultipartBody.Part.createFormData("report", file, RequestBody.create(reports.get(file), MultipartBody.FORM)));
        }


        Call<ResponseBody> apiRequest = api.uploadExternalReports(
                getTeamscaleProject(),
                EReportFormat.valueOf(getReportFormatId().toUpperCase()),
                null,
                revision,
                true,
                getPartition(),
                getUploadMessage(),
                parts
        );
        try {
            apiRequest.execute();
        } catch (IOException e) {
            // Do nothing
        }
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

        public FormValidation doCheckIncludePattern(@QueryParameter String value)
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

    private static class CoverageCollectingFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {

        private static final long serialVersionUID = 1L;

        private final String[] includes;

        public CoverageCollectingFileCallable(String[] includes) {
            this.includes = includes;
        }


        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }

        @Override
        public Map<String, String> invoke(File directory, VirtualChannel virtualChannel) throws IOException, InterruptedException {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(directory);
            directoryScanner.setIncludes(includes);
            directoryScanner.scan();
            Map<String, String> reports = new HashMap<>();
            for (String file : directoryScanner.getIncludedFiles()) {
                reports.put(file, FileUtils.readFileToString(new File(directory, file), "UTF-8"));
            }
            return reports;
        }
    }
}
