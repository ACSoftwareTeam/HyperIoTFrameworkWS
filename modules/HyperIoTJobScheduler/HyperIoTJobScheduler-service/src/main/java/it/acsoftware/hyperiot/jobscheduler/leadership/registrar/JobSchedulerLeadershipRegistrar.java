package it.acsoftware.hyperiot.jobscheduler.leadership.registrar;

import it.acsoftware.hyperiot.base.api.HyperIoTLeadershipRegistrar;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class JobSchedulerLeadershipRegistrar implements HyperIoTLeadershipRegistrar {

    @Override
    public String getLeadershipPath() {
        return "/jobs/quartz/executor";
    }

}
