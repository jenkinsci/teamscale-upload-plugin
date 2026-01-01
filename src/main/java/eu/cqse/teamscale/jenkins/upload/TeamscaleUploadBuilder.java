package eu.cqse.teamscale.jenkins.upload;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServiceGenerator;
import eu.cqse.teamscale.client.JenkinsConsoleInterceptor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import retrofit2.Call;

/**
 * The Teamscale Jenkins plugin.
 * The inheritance from Notifier marks is as a post build action plugin.
 */
public class TeamscaleUploadBuilder extends Notifier implements SimpleBuildStep {

    /**
     * Matcher for populating the credentials dropdown
     */
    private static final CredentialsMatcher MATCHER =
            CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

    /**
     * Api for uploading files to Teamscale.
     */
    private ITeamscaleService api;

    /**
     * For printing errors to jenkins console.
     */
    public static final String ERROR = "TS-ERROR: ";

    /**
     * For printing info to jenkins console.
     */
    public static final String INFO = "TS-INFO: ";

    private final String url;
    private final String teamscaleProject;
    private final String partition;

    @Nullable
    private String repository;

    private final String uploadMessage;
    private final String includePattern;
    private final String reportFormatId;

    @Nullable
    private String revision;

    private String credentialsId;

    /** {@code null} means inherit from global configuration. */
    @Nullable
    private TeamscaleUploadPluginResult resultNoReports;

    /** {@code null} means inherit from global configuration. */
    @Nullable
    private TeamscaleUploadPluginResult resultOnUploadFailure;

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
    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundConstructor
    public TeamscaleUploadBuilder(
            String url,
            String credentialsId,
            String teamscaleProject,
            String partition,
            String uploadMessage,
            String includePattern,
            String reportFormatId,
            @Nullable String revision) {
        this.url = url;
        this.teamscaleProject = teamscaleProject;
        this.partition = partition;
        this.uploadMessage = uploadMessage;
        this.includePattern = includePattern;
        this.reportFormatId = reportFormatId;
        this.credentialsId = credentialsId;
        this.revision = Util.fixEmpty(revision);
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getUrl() {
        return url;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getTeamscaleProject() {
        return teamscaleProject;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getPartition() {
        return partition;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @Nullable
    public String getRepository() {
        return repository;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getUploadMessage() {
        return uploadMessage;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getIncludePattern() {
        return includePattern;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getReportFormatId() {
        return reportFormatId;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getCredentialsId() {
        return credentialsId;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public @Nullable String getRevision() {
        return revision;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setRevision(@Nullable String revision) {
        this.revision = Util.fixEmpty(revision);
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setRepository(@Nullable String repository) {
        this.repository = Util.fixEmpty(repository);
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @Nullable
    public String getResultNoReports() {
        if (resultNoReports == null) {
            return null;
        }
        return resultNoReports.toString();
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setResultNoReports(String resultOnNoReports) {
        if (Util.fixEmpty(resultOnNoReports) == null) {
            this.resultNoReports = null;
        } else {
            this.resultNoReports = TeamscaleUploadPluginResult.valueOf(resultOnNoReports);
        }
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @Nullable
    public String getResultOnUploadFailure() {
        if (resultOnUploadFailure == null) {
            return null;
        }
        return resultOnUploadFailure.toString();
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setResultOnUploadFailure(String resultOnUploadFailure) {
        if (Util.fixEmpty(resultOnUploadFailure) == null) {
            this.resultOnUploadFailure = null;
        } else {
            this.resultOnUploadFailure = TeamscaleUploadPluginResult.valueOf(resultOnUploadFailure);
        }
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {

        StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(
                credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.fromUri(getUrl()).build());

        if (credential == null) {
            listener.getLogger().println(ERROR + "credentials are null");
            run.setResult(Result.FAILURE);
            return;
        }

        HttpUrl url = HttpUrl.parse(getUrl());
        if (url == null) {
            listener.getLogger().println(ERROR + "Failed to parse URL");
            run.setResult(Result.FAILURE);
            return;
        }

        api = TeamscaleServiceGenerator.createService(
                ITeamscaleService.class,
                url,
                credential.getUsername(),
                credential.getPassword().getPlainText(),
                Duration.ofSeconds(60),
                Duration.ofSeconds(60),
                new JenkinsConsoleInterceptor(listener.getLogger()));

        String rev = getScmRevision(env);
        listener.getLogger().println(INFO + "revision: " + rev);
        if (rev == null) {
            listener.getLogger()
                    .println(ERROR + "Could not find any revision. Currently only GIT and SVN are supported.");
            run.setResult(Result.FAILURE);
            return;
        }

        Map<String, String> reports =
                workspace.act(new CoverageCollectingFileCallable(new String[] {getIncludePattern()}));

        if (reports.isEmpty()) {
            TeamscaleUploadPluginResult resultingResultNoReports = resultNoReports;
            if (resultingResultNoReports == null) {
                // If the job is set to inherit, use global configuration
                resultingResultNoReports =
                        TeamscaleUploadPluginConfiguration.get().getResultNoReportsEnum();
            }
            String level = ERROR;
            switch (resultingResultNoReports) {
                case IGNORE:
                    level = INFO;
                    break;
                case UNSTABLE:
                    run.setResult(Result.UNSTABLE);
                    break;
                case FAILURE:
                    run.setResult(Result.FAILURE);
                    break;
            }
            listener.getLogger()
                    .println(level + "No files found to upload to Teamscale with pattern \"" + getIncludePattern()
                            + "\"");
            return;
        }
        try {
            uploadReports(reports, rev);
        } catch (IOException e) {
            TeamscaleUploadPluginResult resultingResultOnUploadFailure = resultOnUploadFailure;
            if (resultingResultOnUploadFailure == null) {
                // If the job is set to inherit, use global configuration
                resultingResultOnUploadFailure =
                        TeamscaleUploadPluginConfiguration.get().getResultOnUploadFailureEnum();
            }
            String level = ERROR;
            switch (resultingResultOnUploadFailure) {
                case IGNORE:
                    level = INFO;
                    break;
                case UNSTABLE:
                    run.setResult(Result.UNSTABLE);
                    break;
                case FAILURE:
                    run.setResult(Result.FAILURE);
                    break;
            }
            listener.getLogger().println(level + "Failed to upload reports to Teamscale");
        }
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

    private void uploadReports(Map<String, String> reports, String revision) throws IOException {
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Map.Entry<String, String> filenameAndReportContent : reports.entrySet()) {
            parts.add(MultipartBody.Part.createFormData(
                    "report",
                    filenameAndReportContent.getKey(),
                    RequestBody.create(filenameAndReportContent.getValue(), MultipartBody.FORM)));
        }

        Call<ResponseBody> apiRequest = api.uploadExternalReports(
                getTeamscaleProject(),
                getReportFormatId().toUpperCase(),
                null,
                revision,
                getRepository(),
                true,
                getPartition(),
                getUploadMessage(),
                parts);
        apiRequest.execute();
    }

    /**
     * Description/Hint provided if user does not fill out the plugin fields correctly.
     */
    @Symbol("teamscale")
    @Extension
    @SuppressWarnings("unused") // used by stapler web framework
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckUrl(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            HttpUrl url = HttpUrl.parse(value);
            if (url == null) {
                return FormValidation.error("Invalid URL");
            }
            return FormValidation.ok();
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckTeamscaleProject(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckPartition(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckUploadMessage(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckIncludePattern(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckReportFormatId(@AncestorInPath Item item, @QueryParameter String value)
                throws IOException, ServletException {
            return getFormValidation(value);
        }

        /**
         * Populates the dropdown for the credentials matching {@literal MATCHER}
         *
         * @param item       to look in.
         * @param url           jenkins server url
         * @param credentialsId current populated id
         * @return list of credentials
         */
        @SuppressWarnings("unused") // used by stapler web framework
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item, @QueryParameter String url, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            return result.includeMatchingAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf2((Queue.Task) item) : ACL.SYSTEM2,
                            item,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri(url).build(),
                            MATCHER)
                    .includeCurrentValue(credentialsId);
        }

        @SuppressWarnings("unused") // used by stapler web framework
        public FormValidation doCheckCredentialsId(
                @AncestorInPath Item item, @QueryParameter String url, @QueryParameter String value) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (value.startsWith("${") && value.endsWith("}")) {
                return FormValidation.warning("Cannot validate expression based credentials");
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Upload will fail without credentials");
            }
            if (CredentialsProvider.listCredentialsInItem(
                            StandardUsernamePasswordCredentials.class,
                            item,
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf2((Queue.Task) item) : ACL.SYSTEM2,
                            URIRequirementBuilder.fromUri(url).build(),
                            CredentialsMatchers.withId(value))
                    .isEmpty()) {
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
            if (value.isEmpty()) {
                return FormValidation.error(Messages.TeamscaleBuilder_DescriptorImpl_errors_requiredField());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.TeamscaleBuilder_DescriptorImpl_DisplayName();
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public ListBoxModel doFillResultNoReportsItems(
                @AncestorInPath Item item, @QueryParameter String resultNoReports) {
            ListBoxModel items = new ListBoxModel();
            items.add("inherit", "");
            items.addAll(Arrays.stream(TeamscaleUploadPluginResult.values())
                    .map(e -> new ListBoxModel.Option(e.toString()))
                    .collect(Collectors.toList()));
            return items;
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckResultNoReports(@AncestorInPath Item item, @QueryParameter String value) {
            if (Util.fixEmpty(value) != null
                    && Arrays.stream(TeamscaleUploadPluginResult.values())
                            .noneMatch(a -> a.name().equals(value))) {
                return FormValidation.error("Please specify a correct result when no reports.");
            }
            return FormValidation.ok();
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public ListBoxModel doFillResultOnUploadFailureItems(
                @AncestorInPath Item item, @QueryParameter String resultOnUploadFailure) {
            ListBoxModel items = new ListBoxModel();
            items.add("inherit", "");
            items.addAll(Arrays.stream(TeamscaleUploadPluginResult.values())
                    .map(e -> new ListBoxModel.Option(e.toString()))
                    .collect(Collectors.toList()));
            return items;
        }

        @POST
        @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects, used by stapler web framework
        public FormValidation doCheckResultOnUploadFailure(@AncestorInPath Item item, @QueryParameter String value) {
            if (Util.fixEmpty(value) != null
                    && Arrays.stream(TeamscaleUploadPluginResult.values())
                            .noneMatch(a -> a.name().equals(value))) {
                return FormValidation.error("Please specify a correct result on upload failure.");
            }
            return FormValidation.ok();
        }
    }

    private static class CoverageCollectingFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {

        private static final long serialVersionUID = 1L;

        private final String[] includes;

        public CoverageCollectingFileCallable(String[] includes) {
            this.includes = includes;
        }

        @Override
        public Map<String, String> invoke(File directory, VirtualChannel virtualChannel)
                throws IOException, InterruptedException {
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(directory);
            directoryScanner.setIncludes(includes);
            directoryScanner.scan();
            Map<String, String> reports = new HashMap<>();
            for (String file : directoryScanner.getIncludedFiles()) {
                reports.put(file, FileUtils.readFileToString(new File(directory, file), StandardCharsets.UTF_8));
            }
            return reports;
        }
    }
}
