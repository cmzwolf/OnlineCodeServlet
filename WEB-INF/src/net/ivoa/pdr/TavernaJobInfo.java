package net.ivoa.pdr;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.UserBusiness;
import net.ivoa.pdr.commons.JobBean;
import net.ivoa.pdr.tavernaCommunication02.Errors;
import net.ivoa.pdr.tavernaCommunication02.Inputs;
import net.ivoa.pdr.tavernaCommunication02.JobDetail;
import net.ivoa.pdr.tavernaCommunication02.Outputs;
import net.ivoa.pdr.tavernaCommunication02.Parameter;

public class TavernaJobInfo extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		// Getting the mail of the user
		try {
			String serverResponse = "";

			Integer userIdInDB = UserBusiness.getInstance().getIdUserByMail(
					req.getParameter("mail"));

			Integer jobId = Integer.parseInt(req.getParameter("jobId"));

			Integer userIdForUser = Integer
					.parseInt(req.getParameter("userId"));

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

	private String computeServerResponse(Integer userId, Integer jobId,
			Integer userIdForUser) throws SQLException, ClassNotFoundException {

		String serverResponse = "";

		JobBean job = JobBusiness.getInstance().getJobBeanFromIdJob(jobId);

		String demandDateforUser = JobBusiness.getInstance()
				.getDateWhereUserAskedTheJob(userId, jobId);

		String notificationDate = JobBusiness.getInstance()
				.getDateWhereUserReceiveNotificationForJob(userId, jobId);

		String servletContainer = GlobalTechConfigBusiness.getInstance()
				.getServletContainer();

		if (null == demandDateforUser || demandDateforUser.equalsIgnoreCase("")) {
			// User never asked that job!
			serverResponse = "error: current user and job owner do not correspond";

		} else {
			// The user asked this job;

			JobDetail jobDetail = new JobDetail();
			jobDetail.setJobId(jobId.toString());
			jobDetail.setDemandDate(job.getDemandDate());
			jobDetail.setJobPhase(JobBusiness.getInstance()
					.computeJobPhase(job));
			jobDetail.setFinishingDate(job.getFinishingDate());

			Inputs input = new Inputs();
			Outputs outputs = new Outputs();
			Errors errors = new Errors();

			// Building the inputs
			for (Entry<String, String> param : job.getJobConfiguration()
					.entrySet()) {
				Parameter tempInput = new Parameter();
				tempInput.setName(param.getKey());
				tempInput.setValue(param.getValue());
				input.getParam().add(tempInput);
			}

			// Building the outputs
			int fileNumber = 1;
			for (String resultValue : job.getJobResults()) {
				Parameter tempOutput = new Parameter();
				tempOutput.setName("file" + fileNumber);
				tempOutput.setValue(resultValue);
				outputs.getParam().add(tempOutput);
			}

			// Building the errors
			for (String currentError : job.getJobErrors()) {
				errors.getErrorDetail().add(currentError);
			}

			jobDetail.setInputs(input);
			jobDetail.setOutputs(outputs);
			jobDetail.setErrors(errors);

			// Preparing the JAXB technical files for the unmarshall operation
			JAXBContext jaxbContext;
			try {
				jaxbContext = JAXBContext
						.newInstance("net.ivoa.pdr.tavernaCommunication02");

				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
						new Boolean(true));

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				marshaller.marshal(jobDetail, out);
				serverResponse = new String(out.toByteArray());
			} catch (JAXBException e) {
				e.printStackTrace();
				serverResponse = "JaxB error";
			}
		}
		return serverResponse;
	}

}
