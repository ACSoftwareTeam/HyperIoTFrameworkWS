package it.acsoftware.hyperiot.company.service;

import java.util.logging.Level;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl ;
import it.acsoftware.hyperiot.company.api.CompanyRepository;
import it.acsoftware.hyperiot.company.api.CompanySystemApi;
import it.acsoftware.hyperiot.company.model.Company;

/**
 *
 * @author Aristide Cittadino Implementation class of the CompanySystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = CompanySystemApi.class, immediate = true)
public final class CompanySystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Company>   implements CompanySystemApi {

	/**
	 * Injecting the CompanyRepository to interact with persistence layer
	 */
	private CompanyRepository repository;

	/**
	 * Constructor for a CompanySystemServiceImpl
	 */
	public CompanySystemServiceImpl() {
		super(Company.class);
	}

	/**
	 * Return the current repository
	 */
	protected CompanyRepository getRepository() {
        getLog().log(Level.FINEST, "invoking getRepository, returning: {0}" , this.repository);
		return repository;
	}

	/**
	 * @param companyRepository The current value of CompanyRepository to interact with persistence layer
	 */
	@Reference
	protected void setRepository(CompanyRepository companyRepository) {
        getLog().log(Level.FINEST, "invoking setRepository, setting: {0}" , companyRepository);
		this.repository = companyRepository;
	}


}
