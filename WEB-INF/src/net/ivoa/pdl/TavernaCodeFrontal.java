package net.ivoa.pdl;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import net.ivoa.pdl.tavernaCommunication01.JobInstances;
import net.ivoa.pdl.tavernaCommunication01.JobsList;
import net.ivoa.pdr.business.GlobalTechConfigBusiness;

public class TavernaCodeFrontal extends GenericOnlineCodeFrontal {

	private String tavernaResponse = "";

	@Override
	protected String buildServerResponse() {
		return this.tavernaResponse;
	}

	@Override
	protected void notifyFreshlyCreatedJobs() throws SQLException,
			ClassNotFoundException {
		// We get the service ID
		String serviceId = GlobalTechConfigBusiness.getInstance()
				.getServletContainer();

		// We define the root object of this PDL-Taverna communication
		JobsList communicationCore = new JobsList();
		communicationCore.setServiceName(serviceId);

		JobInstances temp = new JobInstances();
		temp.setJobId(this.CreatedJobId.toString());
		temp.setUserId(this.userId.toString());
		communicationCore.getList().add(temp);

		// Preparing the JAXB technical files for the unmarshall operation
		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext
					.newInstance("net.ivoa.pdl.tavernaCommunication01");

			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					new Boolean(true));

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshal(communicationCore, out);
			this.tavernaResponse = new String(out.toByteArray());
		} catch (JAXBException e) {
			e.printStackTrace();
			this.tavernaResponse = "JaxB error";
		}
	}

}
