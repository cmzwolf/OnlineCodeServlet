package net.ivoa.pdr;

import java.sql.SQLException;

import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.MailSenderBusiness;
import net.ivoa.pdr.commons.JobBean;

public class OnlineCodeFrontal extends GenericOnlineCodeFrontal {

	@Override
	protected String buildServerResponse() {
		return "ok";
	}

	@Override
	protected void notifyFreshlyCreatedJobs() throws SQLException,
			ClassNotFoundException {
		String jobsDescription = "";
		for (Integer idJob : this.freshlyDemandedJobs) {
			JobBean job = JobBusiness.getInstance().getJobBeanFromIdJob(idJob);
			jobsDescription = jobsDescription
					+ JobBusiness.getInstance().describeJobInTextMode(job);
			jobsDescription = jobsDescription
					+ "--------------------------------------------------------\n";
		}
		MailSenderBusiness.getInstance().sendMailNotifingNewJobs(this.userMail,
				this.userId, jobsDescription);
	}

}
