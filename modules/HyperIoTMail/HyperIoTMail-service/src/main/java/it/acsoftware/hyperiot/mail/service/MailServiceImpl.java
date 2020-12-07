package it.acsoftware.hyperiot.mail.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.mail.api.MailApi;
import it.acsoftware.hyperiot.mail.api.MailSystemApi;
import it.acsoftware.hyperiot.mail.model.MailTemplate;

/**
 *
 * @author Aristide Cittadino Implementation class of MailApi interface. It is
 *         used to implement all additional methods in order to interact with
 *         the system layer.
 */
@Component(service = MailApi.class, immediate = true)
public final class MailServiceImpl extends HyperIoTBaseEntityServiceImpl<MailTemplate>
		implements MailApi {

	/**
	 * Injecting the MailSystemApi
	 */
	private MailSystemApi systemService;

	/**
	 * Constructor for a MailServiceImpl
	 */
	public MailServiceImpl() {
		super(MailTemplate.class);
	}

	/**
	 *
	 * @return The current MailSystemApi
	 */
	protected MailSystemApi getSystemService() {
		getLog().log(Level.FINEST, "invoking getSystemService, returning: {0}" , systemService);
		return systemService;
	}

	/**
	 *
	 * @param mailSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(MailSystemApi mailSystemService) {
		getLog().log(Level.FINEST, "invoking setSystemService, setting: {0}" , systemService);
		systemService = mailSystemService;
	}

	@Override
	public void sendMail(String from, List<String> recipients, List<String> ccRecipients,
			List<String> bccRecipients, String subject, String content, List<byte[]> attachments) {
		systemService.sendMail(from, recipients, ccRecipients, bccRecipients, subject, content,
				attachments);

	}

	@Override
	public String generateTextFromTemplate(String templateName, HashMap<String, Object> params) throws IOException {
		return systemService.generateTextFromTemplate(templateName, params);
	}

}
