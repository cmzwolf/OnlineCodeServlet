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

public class getJobInfo extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		// Getting the mail of the user
		try {
			String serverResponse = "";

			Integer userIdInDB = UserBusiness.getInstance().getIdUserByMail(
					req.getParameter("mail"));

			Integer jobId = Integer.parseInt(req.getParameter("jobId"));
			
			Integer userIdForUser = Integer.parseInt(req.getParameter("userId"));
			
			
			serverResponse = computeServerResponse(userIdInDB, jobId,userIdForUser);

			rep.setContentType("text/html");

			PrintWriter page;
			page = rep.getWriter();
			page.println(serverResponse);
			page.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String computeServerResponse(Integer userId, Integer jobId, Integer userIdForUser)
			throws SQLException, ClassNotFoundException {

		String serverResponse = "";

		JobBean job = JobBusiness.getInstance().getJobBeanFromIdJob(jobId);

		String demandDateforUser = JobBusiness.getInstance()
				.getDateWhereUserAskedTheJob(userId, jobId);

		String notificationDate = JobBusiness.getInstance()
				.getDateWhereUserReceiveNotificationForJob(userId, jobId);
		
		
		String servletContainer = GlobalTechConfigBusiness.getInstance().getServletContainer();
		
		if (null == demandDateforUser || demandDateforUser.equalsIgnoreCase("")) {
			// User never asked that job!
			serverResponse += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			serverResponse += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
			serverResponse += "<head>\n";
			serverResponse += "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
			serverResponse += "<title>" + "Result for the job whose Id is "
					+ jobId + "</title>";
			serverResponse += "<link href=\""+ servletContainer + "css/style.css\" " +  "rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />";
			serverResponse += "</head>\n";
			serverResponse += "<body>";
			serverResponse += "<div id=\"jobDescription\">";
			serverResponse += "<p> You never asked the job with the Id "
					+ jobId + ".</p>\n";
			serverResponse += "</body>";
			serverResponse += "</html>";

		} else {
			// The user asked this job;
			serverResponse += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			serverResponse += "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
			serverResponse += "<head>\n";
			serverResponse += "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
			serverResponse += "<title>" + "Result for the job whose Id is "
					+ jobId + "</title>";
			serverResponse += "<link href=\""+ servletContainer + "css/style.css\" " +  "rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />";
			serverResponse += "</head>\n";
			serverResponse += "<body><div id=\"jobDescription\"><h1> Job Details<br></br></h1>";
			serverResponse += "<h2> You asked the job with the following properties on "
					+ demandDateforUser + ".<br></br></h2>\n";
			serverResponse += JobBusiness.getInstance().describeJobInHtmlMode(
					job);
			if (null != notificationDate
					&& !notificationDate.equalsIgnoreCase("")) {
				serverResponse += "<p> You received notification of this job by mail on"
						+ notificationDate + "</p>\n";
			}
			serverResponse += "</div>";
			
			String jobPhase = JobBusiness.getInstance().computeJobPhase(job);
			String stopOrDelete;
			if(jobPhase.equalsIgnoreCase("pending") || jobPhase.equalsIgnoreCase("running")){
				stopOrDelete = "stop";
			}else{
				stopOrDelete = "delete";
			}
			
			String nextActionUrl = servletContainer+"StopJob?jobId="+jobId + "&userId="+userId+ "&userIdForUser="+userIdForUser;
					
			serverResponse += "<div id=\"stopJob\">\n";
			serverResponse += "<a href=\""+ nextActionUrl + "\">"+ stopOrDelete + " this job" +"</a>";
			serverResponse += "</div>";
			
			serverResponse += "</body>";
			serverResponse += "</html>";
		}
		return serverResponse;
	}

	public static void main(String [] args) throws SQLException, ClassNotFoundException{
		System.out.println(new getJobInfo().computeServerResponse(1, 1,1));
	}
}
