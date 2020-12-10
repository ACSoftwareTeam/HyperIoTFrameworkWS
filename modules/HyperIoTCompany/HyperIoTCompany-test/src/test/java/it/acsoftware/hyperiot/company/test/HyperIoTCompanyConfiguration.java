package it.acsoftware.hyperiot.company.test;

import it.acsoftware.hyperiot.company.model.Company;
import org.ops4j.pax.exam.Option;

public class HyperIoTCompanyConfiguration {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String companyResourceName = Company.class.getName();

    protected static Option[] getConfiguration() {
        return new Option[]{};
    }

}
