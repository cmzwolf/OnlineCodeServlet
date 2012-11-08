package net.ivoa.pdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.UserBusiness;

/**
 * @author Carlo Maria Zwolf
 * Observatoire de Paris
 * LERMA
 */

public class StopJob extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		// Getting the mail of the user
		try {
			String serverResponse = "";

			Integer userIdInDB = Integer.parseInt(req.getParameter("userId"));

			Integer jobId = Integer.parseInt(req.getParameter("jobId"));

			Integer userIdForUser = Integer.parseInt(req
					.getParameter("userIdForUser"));

			serverResponse = computeServerResponse(userIdInDB, jobId,
					userIdForUser);

			rep.setContentType("text/html");

			PrintWriter page;
			page = rep.getWriter();
			page.println(serverResponse);
			page.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String computeServerResponse(Integer userIdInDB, Integer jobId,
			Integer userIdForUser) throws MalformedURLException, IOException,
			SQLException, ClassNotFoundException {

		String servletContainer = GlobalTechConfigBusiness.getInstance()
				.getServletContainer();

		if (userIdInDB.equals(userIdForUser)) {
			// delete the link between user and IdJob in Notification List
			UserBusiness.getInstance().cutLinkUserJob(userIdInDB, jobId);

			// then reload the job Summary Page
			String serviceUrl = servletContainer + "JobSummary?mail="
					+ UserBusiness.getInstance().getMailFromUserId(userIdInDB)
					+ "&jobId=" + jobId + "&userIdForUser=" + userIdForUser
					+ "&userId=" + userIdInDB;

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(new URL(serviceUrl).openConnection()
							.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			bufferedReader.close();
			return sb.toString();
		} else {
			String serverResponse = "";
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
			return serverResponse;
		}
	}
}
