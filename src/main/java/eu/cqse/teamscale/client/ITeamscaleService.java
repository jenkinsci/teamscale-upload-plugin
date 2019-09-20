package eu.cqse.teamscale.client;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.*;

import java.io.IOException;

/** {@link Retrofit} API specification for Teamscale. */
public interface ITeamscaleService {

	/** Report upload API. */
	@Multipart
	@POST("p/{projectName}/external-report/")
	Call<ResponseBody> uploadExternalReport(
            @Path("projectName") String projectName,
            @Query("format") EReportFormat format,
            @Query("adjusttimestamp") boolean adjustTimestamp,
            @Query("movetolastcommit") boolean moveToLastCommit,
            @Query("partition") String partition,
            @Query("message") String message,
            @Part("report") RequestBody report
    );

	/**
	 * Uploads the given report body to Teamscale as blocking call
	 * with adjusttimestamp and movetolastcommit set to true.
	 *
	 * @return Returns the request body if successful, otherwise throws an IOException.
	 */
	default String uploadReport(
            String projectName,
            String partition,
            EReportFormat reportFormat,
            String message,
            RequestBody report
    ) throws IOException {
		try {
			Response<ResponseBody> response = uploadExternalReport(
					projectName,
					reportFormat,
					true,
					false,
					partition,
					message,
					report
			).execute();

			ResponseBody body = response.body();
			if (response.isSuccessful()) {
				return body.string();
			}

			String bodyString;
			if (body == null) {
				bodyString = "<no body>";
			} else {
				bodyString = body.string();
			}
			throw new IOException(
					"Request failed with error code " + response.code() + ". Response body:\n" + bodyString);
		} catch (IOException e) {
			throw new IOException("Failed to upload report. " + e.getMessage(), e);
		}
	}
}