package eu.cqse.teamscale.client;

import okhttp3.HttpUrl;

/** Holds Teamscale server details. */
public class TeamscaleServer {

	/** The URL of the Teamscale server. */
	public String url;

	/** The project id within Teamscale. */
	public String project;

	/** The user name used to authenticate against Teamscale. */
	public String userName;

	/** The user's access token. */
	public String userAccessToken;

	/** The partition to upload reports to. */
	public String partition;

	/** The corresponding code commit to which the coverage belongs. */
//	public CommitDescriptor commit;

	/** The commit message shown in the Teamscale UI for the coverage upload. */
	public String message = "Agent coverage upload";

	public TeamscaleServer(String url, String userName, String ideKey, String project, String partition, String uploadMessage){
		this.url = url;
		this.userName = userName;
		this.userAccessToken = ideKey;
		this.project = project;
		this.partition = partition;
		this.message = uploadMessage;
	}

	/** Returns if all required fields are non-null. */
	public boolean hasAllRequiredFieldsSet() {
		return url != null &&
				project != null &&
				userName != null &&
				userAccessToken != null &&
				partition != null ;
	}

	/** Returns whether all required fields are null. */
	public boolean hasAllRequiredFieldsNull() {
		return url == null &&
				project == null &&
				userName == null &&
				userAccessToken == null &&
				partition == null;
	}

	@Override
	public String toString() {
		return "Teamscale " + url + " as user " + userName + " for " + project + " to " + partition + " at " ;
	}
}
