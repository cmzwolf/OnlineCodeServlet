package net.ivoa.pdr;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.UserBusiness;
import net.ivoa.pdr.commons.JobBean;

/**
 * @author Carlo Maria Zwolf
 * Observatoire de Paris
 * LERMA
 */

public class JobSummary extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		// Getting the mail of the user
		try {
			String serverResponse = "";

			String userMail = req.getParameter("mail");

			Integer userIdForUser = Integer
					.parseInt(req.getParameter("userIdForUser"));

			Integer userIdInDB = UserBusiness.getInstance().getIdUserByMail(
					req.getParameter("mail"));

			serverResponse = computeServerResponse(userMail, userIdForUser,
					userIdInDB);

			PrintWriter page;
			page = rep.getWriter();
			page.println(serverResponse);
			page.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String computeServerResponse(String userMail,
			Integer userIdForUser, Integer userIdInDB) throws SQLException,
			ClassNotFoundException {

		String servletContainer = GlobalTechConfigBusiness.getInstance()
				.getServletContainer();

		String serverResponse = "";

		// If the userId In DB corresponds to the user id provided by User
		if (userIdForUser.equals(userIdInDB)) {
			serverResponse += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			serverResponse += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
			serverResponse += "<head>\n";
			serverResponse += "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
			serverResponse += "<title>" + "Job list for user " + userMail
					+ "</title>";
			serverResponse += "<link href=\""
					+ servletContainer
					+ "css/style.css\" "
					+ "rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />";
			serverResponse += "</head>\n";

			serverResponse += "<body><div id=\"jobDescription\"><h1> List of all your jobs<br></br></h1>";
			serverResponse += "<h2> You asked the following jobs "
					+ ".<br></br></h2>\n";

			serverResponse += "<div id=\"jobHead\">\n";
			serverResponse += "<br></br>";
			serverResponse += "<table border = \"1\" cellpadding=\"10\" cellspacing=\"10\">";
			serverResponse += "<tr><th> Job Id </th><th> Phase </th><th> Demand Date </th><th> Job Details </th></tr>\n";
			for (Integer currentJobId : JobBusiness.getInstance()
					.getListOfJobsAskedByUser(userIdInDB)) {
				JobBean currentJob = JobBusiness.getInstance()
						.getJobBeanFromIdJob(currentJobId);
				String demandDate = JobBusiness.getInstance()
						.getDateWhereUserAskedTheJob(userIdInDB, currentJobId);
				String phase = JobBusiness.getInstance().computeJobPhase(
						currentJob);
				String detailLink = servletContainer + "getJobInfo?mail="
						+ userMail + "&jobId=" + currentJobId + "&userId="
						+ userIdForUser;

				serverResponse += "<tr><td> " + currentJobId + " </td><td> "
						+ phase + " </td><td> " + demandDate
						+ " </td><td> <a href=\"" + detailLink + "\">"
						+ " See Job Details </a>" + "</td></tr>";

			}
			serverResponse += "</table>";
			serverResponse += "</div>";

			serverResponse += "</div>";
			serverResponse += "</body>";
			serverResponse += "</html>";

		} else {
			serverResponse += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			serverResponse += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
			serverResponse += "<head>\n";
			serverResponse += "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
			serverResponse += "<title>" + "Job list Error" + "</title>";
			serverResponse += "<link href=\""
					+ servletContainer
					+ "css/style.css\" "
					+ "rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />";
			serverResponse += "</head>\n";
			serverResponse += "<body>";
			serverResponse += "<div id=\"jobDescription\">";
			serverResponse += "<p> User provided mail and user Id do not correspond in our database "
					+ ".</p>\n";
			serverResponse += "</body>";
			serverResponse += "</html>";
		}
		return serverResponse;
	}
}
