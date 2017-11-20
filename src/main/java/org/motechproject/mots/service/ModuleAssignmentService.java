package org.motechproject.mots.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.motechproject.mots.domain.AssignedModules;
import org.motechproject.mots.domain.Module;
import org.motechproject.mots.repository.AssignedModulesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModuleAssignmentService {

  @Autowired
  private AssignedModulesRepository repository;

  @Autowired
  private IvrService ivrService;

  /**
   * Assign modules to CHW.
   * @param assignedModules modules assigned for CHW
   */
  public void assignModules(AssignedModules assignedModules) {
    AssignedModules existingAssignedModules =
        repository.findByHealthWorkerId(assignedModules.getHealthWorker().getId());

    if (existingAssignedModules != null) {
      existingAssignedModules.setModules(assignedModules.getModules());
    } else {
      existingAssignedModules = assignedModules;
    }

    repository.save(existingAssignedModules);

    String ivrId = existingAssignedModules.getHealthWorker().getIvrId();

    if (ivrId != null) {
      ivrService.addSubscriberToGroups(ivrId,
          getIvrGroupsFromModules(existingAssignedModules.getModules()));
    }
  }

  private List<String> getIvrGroupsFromModules(Set<Module> modules) {
    List<String> ivrGroups = new ArrayList<>();

    for (Module module: modules) {
      if (module.getIvrGroup() != null) {
        ivrGroups.add(module.getIvrGroup());
      }
    }

    return ivrGroups;
  }
}