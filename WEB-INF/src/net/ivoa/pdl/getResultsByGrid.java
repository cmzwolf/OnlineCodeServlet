package net.ivoa.pdl;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.UserBusiness;

public class getResultsByGrid extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		// Getting the mail of the user
		try {
			String serverResponse = "";

			Integer userIdInDB = UserBusiness.getInstance().getIdUserByMail(
					req.getParameter("mail"));

			Integer userIdForUser = Integer
					.parseInt(req.getParameter("userId"));
			
			String gridId = req.getParameter("gridId");
			
			serverResponse = computeServerResponse(userIdInDB, userIdForUser, gridId);

			rep.setContentType("text/plain");

			PrintWriter page;
			page = rep.getWriter();
			page.println(serverResponse);
			page.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String computeServerResponse(Integer userIdInDB,
			Integer userIdForUser, String gridId) throws ClassNotFoundException, SQLException {
		String toReturn = "";
		
		if (userIdInDB != userIdForUser){
			return "";
		}else{
			List<Map<String,String>> results = JobBusiness.getInstance().getFinishedResultsByGridAndUser(gridId,userIdInDB);
			for(Map<String,String> currentResult : results){
				for(Map.Entry<String, String> entry : currentResult.entrySet()){
					toReturn = toReturn + entry.getValue() + "\n";
				}
			}
			
			return toReturn;
		}
		
	}

}
