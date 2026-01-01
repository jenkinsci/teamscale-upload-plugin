package eu.cqse.teamscale.jenkins.upload;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.Arrays;
import java.util.stream.Collectors;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class TeamscaleUploadPluginConfiguration extends GlobalConfiguration {

    public static TeamscaleUploadPluginConfiguration get() {
        return ExtensionList.lookupSingleton(TeamscaleUploadPluginConfiguration.class);
    }

    private TeamscaleUploadPluginResult resultNoReports;

    private TeamscaleUploadPluginResult resultOnUploadFailure;

    public TeamscaleUploadPluginConfiguration() {
        resultNoReports = TeamscaleUploadPluginResult.IGNORE;
        resultOnUploadFailure = TeamscaleUploadPluginResult.IGNORE;
        load();
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getResultOnUploadFailure() {
        return resultOnUploadFailure.toString();
    }

    public TeamscaleUploadPluginResult getResultOnUploadFailureEnum() {
        return resultOnUploadFailure;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setResultOnUploadFailure(String resultOnUploadFailure) {
        this.resultOnUploadFailure = TeamscaleUploadPluginResult.valueOf(resultOnUploadFailure);
        save();
    }

    @POST
    @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects
    public ListBoxModel doFillResultOnUploadFailureItems() {
        return new ListBoxModel(Arrays.stream(TeamscaleUploadPluginResult.values())
                .map(e -> new ListBoxModel.Option(e.toString()))
                .collect(Collectors.toList()));
    }

    @POST
    @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects
    public FormValidation doCheckResultOnUploadFailure(@QueryParameter String value) {
        if (Arrays.stream(TeamscaleUploadPluginResult.values())
                .noneMatch(a -> a.name().equals(value))) {
            return FormValidation.error("Please specify a correct result on upload failure.");
        }
        return FormValidation.ok();
    }

    @SuppressWarnings("unused") // used by stapler web framework
    public String getResultNoReports() {
        return resultNoReports.toString();
    }

    public TeamscaleUploadPluginResult getResultNoReportsEnum() {
        return resultNoReports;
    }

    @SuppressWarnings("unused") // used by stapler web framework
    @DataBoundSetter
    public void setResultNoReports(String resultOnNoReports) {
        this.resultNoReports = TeamscaleUploadPluginResult.valueOf(resultOnNoReports);
        save();
    }

    @POST
    @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects
    public ListBoxModel doFillResultNoReportsItems() {
        return new ListBoxModel(Arrays.stream(TeamscaleUploadPluginResult.values())
                .map(e -> new ListBoxModel.Option(e.toString()))
                .collect(Collectors.toList()));
    }

    @POST
    @SuppressWarnings({"lgtm[jenkins/no-permission-check]", "unused"}) // secure because no side effects
    public FormValidation doCheckResultNoReports(@QueryParameter String value) {
        if (Arrays.stream(TeamscaleUploadPluginResult.values())
                .noneMatch(a -> a.name().equals(value))) {
            return FormValidation.error("Please specify a correct result when no reports.");
        }
        return FormValidation.ok();
    }
}
