package net.ivoa.pdl;

import java.sql.SQLException;
import java.util.Map.Entry;

import net.ivoa.pdl.utils.ErrorDetail;
import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.MailSenderBusiness;
import net.ivoa.pdr.commons.JobBean;
import CommonsObjects.GeneralParameter;

public class OnlineCodeFrontal extends GenericOnlineCodeFrontal {

	@Override
	protected String buildServerResponse() {
		
		return "ok";
	}

	@Override
	protected void notifyFreshlyCreatedJobs() throws SQLException,
			ClassNotFoundException {
		if (this.MailRequested) {
			String jobsDescription = "";
			String message = "";

			if (this.errorList.size() > 0) {
				// if there are errors the job has not been created server-side
				message = "Hello,\n\n";
				message += "the Job you just sumbmitted with the following parameters:\n";
				for (Entry<String, GeneralParameter> entry : this.userProvidedData
						.entrySet()) {
					message+="parameter Name: "+entry.getKey() + " and parameter Value ="+entry.getValue()+"\n\n";
				}
				message+="contains the following errors:";
				for(ErrorDetail error : this.errorList){
					message+=error.getErrorMessage()+"\n";
				}
			} else {

				message = "Hello,\n\n";

				/*message += "Please visit the link "
						+ GlobalTechConfigBusiness.getInstance()
								.getServletContainer() + "JobSummary?mail="
						+ userMail + "&userIdForUser=" + userId
						+ " for job administration.\n\n";*/
				
				message += "Please visit the following link for job administration:\n"
						+ GlobalTechConfigBusiness.getInstance()
						.getGWTContainer() + "?userId="+userId+"&mail="+userMail;

				message += "\n Computation demands have been just been recorded for the following jobs:\n";

				JobBean job = JobBusiness.getInstance().getJobBeanFromIdJob(
						this.CreatedJobId);
				jobsDescription = jobsDescription
						+ JobBusiness.getInstance().describeJobInTextMode(job);
				jobsDescription = jobsDescription
						+ "--------------------------------------------------------\n";

				message = message + jobsDescription;
				// If the user want to receive a mail, we sent it
			}

			MailSenderBusiness.getInstance().sendMailNotifingNewJobs(
					this.userMail, this.userId, message);
		}

	}

}
