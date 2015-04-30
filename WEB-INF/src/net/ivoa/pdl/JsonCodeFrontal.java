package net.ivoa.pdl;

import java.sql.SQLException;
import java.util.List;

import net.ivoa.pdl.utils.ErrorDetail;
import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.OutputsBusiness;
import net.ivoa.pdr.commons.IOFile;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonCodeFrontal extends GenericOnlineCodeFrontal {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String JsonResponse;
	
	@Override
	protected String buildServerResponse() {
		return this.JsonResponse;
	}

	@Override
	protected void notifyFreshlyCreatedJobs() throws SQLException,
			ClassNotFoundException {
		// If there are errors
		if (null != this.errorList || this.errorList.size() > 0) {
			// we notify to user the errors
			JSONArray jsonErrorList = new JSONArray();

			for (ErrorDetail error : this.errorList) {
				JSONObject obj = new JSONObject();
				JSONArray list = new JSONArray();

				for (String name : error.getParamNames()) {
					list.add(name);
				}
				obj.put("errorMessage", error.getErrorMessage());
				obj.put("involvedParameter(s)", list);
				jsonErrorList.add(obj);
			}
			this.JsonResponse = jsonErrorList.toJSONString();
		}
		// if there are no errors
		if (null == this.errorList || this.errorList.size() <= 0) {
			// we get the service ID
			String serviceId = GlobalTechConfigBusiness.getInstance()
					.getServletContainer();

			String jobManagementURL = GlobalTechConfigBusiness.getInstance()
					.getGWTContainer()
					+ "userId="
					+ this.userId
					+ "&mail="
					+ this.userMail;

			JSONArray results = new JSONArray();
			List<IOFile> outputFileList = OutputsBusiness.getInstance()
					.getPatternOutputFile();
			for (IOFile outputFile : outputFileList) {
				String tempFileUrl = GlobalTechConfigBusiness.getInstance()
						.getServletContainer()
						+ "/output/"
						+ this.CreatedJobId
						+ "." + outputFile.getFileExtension();
				results.add(tempFileUrl);
			}

			JSONObject returnedMessage = new JSONObject();
			returnedMessage.put("ServiceId", serviceId);
			returnedMessage.put("JobID", this.CreatedJobId);
			returnedMessage.put("UserID", this.userId);
			returnedMessage.put("UserMail", this.userMail);
			returnedMessage.put("ManagementURL", jobManagementURL);
			returnedMessage.put("ExpectedResultsURLs", results);
			
			this.JsonResponse = returnedMessage.toString();
		}
	}

}
