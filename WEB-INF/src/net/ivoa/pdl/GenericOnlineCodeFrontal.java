package net.ivoa.pdl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.ivoa.parameter.model.ParameterDependency;
import net.ivoa.parameter.model.Service;
import net.ivoa.parameter.model.SingleParameter;
import net.ivoa.pdl.interpreter.conditionalStatement.StatementHelperContainer;
import net.ivoa.pdl.interpreter.groupInterpreter.GroupHandlerHelper;
import net.ivoa.pdl.interpreter.groupInterpreter.GroupProcessor;
import net.ivoa.pdl.interpreter.utilities.UserMapper;
import net.ivoa.pdl.interpreter.utilities.Utilities;
import net.ivoa.pdl.utils.ErrorDetail;
import net.ivoa.pdr.business.GlobalTechConfigBusiness;
import net.ivoa.pdr.business.ParametersBusiness;
import net.ivoa.pdr.business.UserBusiness;
import net.ivoa.pdr.commons.ParamConfiguration;
import visitors.GeneralParameterVisitor;
import CommonsObjects.GeneralParameter;

/**
 * @author Carlo Maria Zwolf Observatoire de Paris LERMA
 */

public abstract class GenericOnlineCodeFrontal extends HttpServlet {

	private static final String SEPARATOR = "%";

	private String errorMessage = "";

	protected String serverResponse = "";

	protected Integer userId;
	protected Integer CreatedJobId;

	private String gridID;
	private String jobNickName;
	protected Boolean MailRequested;

	protected Map<String, GeneralParameter> userProvidedData;
	protected List<ErrorDetail> errorList;
	protected String userMail;

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		try {

			this.errorList = new ArrayList<ErrorDetail>();

			System.out.println("*********" + req.getQueryString());

			// Getting the mail of the user
			this.userMail = req.getParameter("mail");
			this.userId = UserBusiness.getInstance().getIdUserByMail(
					this.userMail);

			// Get the data values from user request, according to the PDL
			// description
			this.getRawDataFromUserRequest(req);

			this.processFinalParameters();

			this.notifyFreshlyCreatedJobs();

			rep.setContentType("text/html");

			this.serverResponse = buildServerResponse();

			PrintWriter page;
			page = rep.getWriter();
			page.println(this.serverResponse);
			page.close();

		} catch (Exception e) {
			rep.setContentType("text/html");
			try {
				PrintWriter page;
				page = rep.getWriter();
				page.println("errors");
				page.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			System.out
					.println("!!!!!!!!! Something went wrong in the servlet !!!!!!!!!!");
			e.printStackTrace();
		}
	}

	protected abstract String buildServerResponse();

	protected void processFinalParameters() throws SQLException,
			ClassNotFoundException {

		ParamConfiguration conf = new ParamConfiguration(
				this.convertMapOfGPintoString(userProvidedData));
		
		// We store the job only if it contains no error
		if(this.errorList.size()<=0){
			this.CreatedJobId = ParametersBusiness.getInstance()
					.persistConfigurationAndGetId(conf, this.userId, this.gridID,
							this.jobNickName, this.MailRequested);
		}
	}

	private Map<String, String> convertMapOfGPintoString(
			Map<String, GeneralParameter> mapToConvert) {
		Map<String, String> convertedMap = new HashMap<String, String>();
		for (Map.Entry<String, GeneralParameter> entry : mapToConvert
				.entrySet()) {
			convertedMap.put(entry.getKey(), entry.getValue().getValue());
		}
		return convertedMap;
	}

	protected void getRawDataFromUserRequest(HttpServletRequest req)
			throws ClassNotFoundException, SQLException {
		this.userProvidedData = new HashMap<String, GeneralParameter>();

		// trying to get the optional parameter gridId
		try {
			this.gridID = req.getParameter("gridID");
		} catch (Exception e) {
			this.gridID = "None";
		}

		// trying to get the optional parameter jobNickName
		try {
			this.jobNickName = req.getParameter("jobNickName");
		} catch (Exception e) {
			this.jobNickName = "None";
		}

		// trying to get the optional parameter MailRequested
		try {
			String mailFlag = req.getParameter("MailRequested");
			if (mailFlag.equalsIgnoreCase("false")) {
				this.MailRequested = false;
			} else {
				this.MailRequested = true;
			}
		} catch (Exception e) {
			// By default, we sent mail
			this.MailRequested = true;
		}

		String pdlDescriptionUrl = GlobalTechConfigBusiness.getInstance()
				.getServletContainer() + "pdlDescription/PDL-Description.xml";

		// defining the mapper object for containing the values provided by
		// the user
		UserMapper currentMapper = new UserMapper();

		try {
			// building the model object corresponding to the transmitted file
			// description
			Service service = buildPDLModelObjectFromDescription(pdlDescriptionUrl);

			// Storing this description into the utility static field
			Utilities.getInstance().setService(service);

			// Initializing the GeneralParameterVisitor, for defining
			// GeneralParameters objects
			GeneralParameterVisitor visitor = new GeneralParameterVisitor();

			// getting the value transmitted by the users for the parameters
			for (SingleParameter currentParam : service.getParameters()
					.getParameter()) {
				String parameterName = currentParam.getName();

				// getting the values provided by user
				try {
					// try to get the value
					String parameterValue = req.getParameter(parameterName);

					try {
						// validating the type for parameters
						GeneralParameter gp = new GeneralParameter(
								parameterValue,
								currentParam.getParameterType(),
								currentParam.getSkosConcept(), visitor);

						currentMapper.setSingleValueInMap(parameterName, gp);
						this.userProvidedData.put(parameterName, gp);

					} catch (Exception e) {
						// if the general parameter can not be instantiated, we
						// add
						// this error to the list of errors
						ErrorDetail currentError = new ErrorDetail(
								parameterName, "cannot cast value "
										+ parameterValue
										+ " to type "
										+ currentParam.getParameterType()
												.toString());
						this.errorList.add(currentError);
					}

				} catch (Exception e) {
					// if the parameter is required
					if (currentParam.getDependency().equals(
							ParameterDependency.REQUIRED)) {
						ErrorDetail currentError = new ErrorDetail(
								parameterName,
								"required parameter not provided");
						this.errorList.add(currentError);
					}
					// if the parameter is optional, we do nothing
				}
			}

			performGroupsVerfication(service, currentMapper);

		} catch (Exception e) {
			this.errorList
					.add(new ErrorDetail(
							"PDLdescription",
							"cannot instanciate the object model corresponding to the provided PDL description. Please check your PDL file"));
		}
	}

	
	
	private Service buildPDLModelObjectFromDescription(String pdlDescriptionUrl)
			throws MalformedURLException, IOException, SQLException,
			ClassNotFoundException, JAXBException {

		System.out.println("fetching file == " + pdlDescriptionUrl);

		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new URL(pdlDescriptionUrl)
						.openConnection().getInputStream()));

		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		bufferedReader.close();
		String xmlMessage = sb.toString();

		ByteArrayInputStream input = new ByteArrayInputStream(
				xmlMessage.getBytes());

		JAXBContext jaxbContext = JAXBContext
				.newInstance("net.ivoa.parameter.model");

		Unmarshaller u = jaxbContext.createUnmarshaller();

		return (Service) u.unmarshal(input);
	}

	private void performGroupsVerfication(Service service, UserMapper mapper) {
		try {
			Utilities.getInstance().setMapper(mapper);

			// Defining the PDL verificator
			GroupProcessor groupProcessor = new GroupProcessor(service);

			// processing the verifications
			groupProcessor.process();

			List<GroupHandlerHelper> handlers = groupProcessor
					.getGroupsHandler();
			for (GroupHandlerHelper currentHandler : handlers) {
				List<StatementHelperContainer> shc = currentHandler
						.getStatementHelperList();

				List<String> paramInGroup = new ArrayList<String>();
				for (SingleParameter param : currentHandler
						.getSingleParamIntoThisGroup()) {
					paramInGroup.add(param.getName());
				}

				if (null != shc) {
					for (int i = 0; i < shc.size(); i++) {
						String comment = shc.get(i).getStatementComment();
						Boolean isActivated = shc.get(i).isStatementSwitched();
						Boolean isValid = shc.get(i).isStatementValid();

						if (isActivated && !isValid) {
							ErrorDetail currentError = new ErrorDetail(
									paramInGroup,
									"the following condition is not verified in the"
											+ currentHandler.getGroupName()
											+ "group: " + comment);
							this.errorList.add(currentError);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected abstract void notifyFreshlyCreatedJobs() throws SQLException,
			ClassNotFoundException;

}
