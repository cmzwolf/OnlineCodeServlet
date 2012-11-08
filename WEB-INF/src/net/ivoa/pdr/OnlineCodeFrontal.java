package net.ivoa.pdr;

import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ivoa.pdr.business.JobBusiness;
import net.ivoa.pdr.business.MailSenderBusiness;
import net.ivoa.pdr.business.ParametersBusiness;
import net.ivoa.pdr.business.PurgeBusiness;
import net.ivoa.pdr.business.RawParameterBusiness;
import net.ivoa.pdr.business.ServiceBusiness;
import net.ivoa.pdr.business.UserBusiness;
import net.ivoa.pdr.commons.JobBean;
import net.ivoa.pdr.commons.ParamConfiguration;
import net.ivoa.pdr.utils.PDRMap;
import visitors.GeneralParameterVisitor;
import CommonsObjects.GeneralParameter;

/**
 * @author Carlo Maria Zwolf
 * Observatoire de Paris
 * LERMA
 */

public class OnlineCodeFrontal extends HttpServlet {

	private static final String SEPARATOR = "/";

	private String errorMessage = "";

	private Integer userId;

	private Map<String, GeneralParameter> defaultRawParameters;
	private Map<String, GeneralParameter> userProvidedRawData;
	private List<Integer> freshlyDemandedJobs;
	private String userMail;

	private Map<String, List<String>> userProvidedProcessedParameters;

	public void doGet(HttpServletRequest req, HttpServletResponse rep) {
		try {
			// Getting the mail of the user
			this.userMail = req.getParameter("mail");
			this.userId = UserBusiness.getInstance().getIdUserByMail(
					this.userMail);

			// Obtenir la liste des paramètres par défaut (on parle ici de raw
			// data) depuis la bdd
			this.defaultRawParameters = RawParameterBusiness.getInstance()
					.getRawParameters();

			// On récupère les données brutes depuis le requete de l'utilisateur
			this.getRawDataFromUserRequest(req);

			// On traite la donnée brute pour fabriquer une map de lists
			this.processRawParameter();

			this.processFinalParameters();

			this.notifyFreshlyCreatedJobs();

			rep.setContentType("text/html");

			PrintWriter page;
			page = rep.getWriter();
			page.println("ok");
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

	protected void processFinalParameters() throws SQLException,
			ClassNotFoundException {
		this.freshlyDemandedJobs = new ArrayList<Integer>();
		Integer maxAutorizedSims = ServiceBusiness.getInstance()
				.getCurrentService().getMaxAuthorizedSimulations();
		Integer requiredSimulations = this.computeRequiredSimulation();
		if (requiredSimulations > maxAutorizedSims) {
			this.errorMessage = "too many simulation required, please check the set of your parameters";
		} else {
			List<ParamConfiguration> configurationsList = this
					.convertPDRMapToParamConfiguration(this
							.tensorialProductOfMap(userProvidedProcessedParameters));
			for (ParamConfiguration conf : configurationsList) {
				this.freshlyDemandedJobs.add(ParametersBusiness.getInstance()
						.persistConfigurationAndGetId(conf, this.userId));
			}
			// On appelle ici la purge pour éliminer les configs qui ne
			// respectent pas les contraintes de min et maw
			PurgeBusiness.getInstance().purgeJobOutOfLimits();

		}
	}

	private Integer computeRequiredSimulation() {
		Integer number = 1;
		for (Map.Entry<String, List<String>> entry : this.userProvidedProcessedParameters
				.entrySet()) {
			number = number * entry.getValue().size();
		}
		return number;
	}

	private List<ParamConfiguration> convertPDRMapToParamConfiguration(
			PDRMap paramMap) {

		String[] paramNames = paramMap.getKey().split(SEPARATOR);
		List<String[]> temp = new ArrayList<String[]>();
		for (int i = 0; i < paramMap.getValues().size(); i++) {
			temp.add(paramMap.getValues().get(i).split(SEPARATOR));
		}

		List<ParamConfiguration> toReturn = new ArrayList<ParamConfiguration>();

		for (int j = 0; j < temp.size(); j++) {
			Map<String, String> tempMap = new HashMap<String, String>();
			for (int i = 0; i < paramNames.length; i++) {
				tempMap.put(paramNames[i], temp.get(j)[i]);
			}
			toReturn.add(new ParamConfiguration(tempMap));
		}
		return toReturn;
	}

	protected void processRawParameter() {
		this.userProvidedProcessedParameters = new HashMap<String, List<String>>();
		for (Map.Entry<String, GeneralParameter> entry : this.userProvidedRawData
				.entrySet()) {

			// Si le paramètre de type raw corresponds au paramètre du moteur de
			// calcul
			if (entry.getKey().equalsIgnoreCase(
					entry.getValue().getDescription())) {
				this.userProvidedProcessedParameters
						.put(entry.getKey(), this.buildListFromOneValue(entry
								.getValue().getValue()));
			} else {

				String paramName = entry.getValue().getDescription();

				Integer N = Integer.parseInt(this.userProvidedRawData.get(
						"N" + paramName).getValue());

				Double delta = Double.parseDouble(this.userProvidedRawData.get(
						"delta" + paramName).getValue());

				Double inf = Double.parseDouble(this.userProvidedRawData.get(
						paramName + "Inf").getValue());

				String variationMethod = this.userProvidedRawData.get(
						paramName + "VariationMethod").getValue();

				List<String> toAdd = new ArrayList<String>();

				for (int i = 0; i <= N; i++) {
					if (variationMethod.equalsIgnoreCase("lin")) {
						Double temp = inf + i * delta;
						toAdd.add("" + temp);
					}
					if (variationMethod.equalsIgnoreCase("exp")) {
						Double temp = inf + Math.exp(i * delta);
						toAdd.add("" + temp);
					}
					if (variationMethod.equalsIgnoreCase("log")) {
						Double temp = inf + Math.log(i * delta);
						toAdd.add("" + temp);
					}
				}
				this.userProvidedProcessedParameters.put(paramName, toAdd);
			}
		}
	}

	private List<String> buildListFromOneValue(String input) {
		List<String> toReturn = new ArrayList<String>();
		toReturn.add(input);
		return toReturn;
	}

	protected void getRawDataFromUserRequest(HttpServletRequest req) {
		this.userProvidedRawData = new HashMap<String, GeneralParameter>();
		for (Map.Entry<String, GeneralParameter> entry : this.defaultRawParameters
				.entrySet()) {
			try {

				String submittedParamValue = req.getParameter(entry.getKey());

				if (null == submittedParamValue
						|| "".equalsIgnoreCase(submittedParamValue)) {
					throw new InvalidParameterException();
				}

				GeneralParameter userProvidedRawParam = new GeneralParameter(
						submittedParamValue, entry.getValue().getType(), entry
								.getValue().getDescription(),
						new GeneralParameterVisitor());

				this.userProvidedRawData.put(entry.getKey(),
						userProvidedRawParam);
			} catch (Exception e) {
				this.userProvidedRawData.put(entry.getKey(), entry.getValue());
			}
		}
	}

	protected PDRMap tensorialProductOfMap(Map<String, List<String>> map) {
		// On convertit la map en deux Listes

		List<PDRMap> pdrMap = new ArrayList<PDRMap>();

		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			pdrMap.add(new PDRMap(entry.getKey(), entry.getValue()));
		}

		PDRMap result = new PDRMap(pdrMap.get(0).getKey(), pdrMap.get(0)
				.getValues());

		for (int i = 1; i < pdrMap.size(); i++) {
			result = tensorialProduct(result, pdrMap.get(i));
		}
		return result;
	}

	private PDRMap tensorialProduct(PDRMap first, PDRMap second) {
		String newKey = first.getKey() + SEPARATOR + second.getKey();

		List<String> newValue = new ArrayList<String>();
		for (int i = 0; i < first.getValues().size(); i++) {
			for (int j = 0; j < second.getValues().size(); j++) {
				newValue.add(first.getValues().get(i) + SEPARATOR
						+ second.getValues().get(j));
			}
		}
		return new PDRMap(newKey, newValue);
	}

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

	public static void main(String[] args) throws SQLException,
			ClassNotFoundException {

		OnlineCodeFrontal frontal = new OnlineCodeFrontal();

		// Obtenir la liste des paramètres par défaut (on parle ici de raw
		// data) depuis la bdd
		frontal.defaultRawParameters = RawParameterBusiness.getInstance()
				.getRawParameters();

		frontal.userProvidedRawData = frontal.defaultRawParameters;

		// On traite la donnée brute pour fabriquer une map de lists
		frontal.processRawParameter();
		frontal.userId = UserBusiness.getInstance().getIdUserByMail(
				"carlo-maria.zwolf@obspm.fr");
		frontal.userMail = "carlo-maria.zwolf@obspm.fr";
		frontal.processFinalParameters();
		frontal.processFinalParameters();
		frontal.notifyFreshlyCreatedJobs();
	}
}
