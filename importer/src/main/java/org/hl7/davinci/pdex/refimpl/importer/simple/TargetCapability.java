package org.hl7.davinci.pdex.refimpl.importer.simple;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Optional;

class TargetCapability {

    private CapabilityStatement statement;
    private List<String> excludedResources;


    TargetCapability(CapabilityStatement statement, List<String> excludedResources) {
        this.statement = statement;
        this.excludedResources = excludedResources;
    }

    boolean canPersist(Resource resource) {
        String resourceName = resource.getClass().getSimpleName();
        Optional<CapabilityStatement.CapabilityStatementRestResourceComponent> resourceDescription = getCapabilityResources(resourceName);
        if (resourceDescription.isPresent() && !excludedResources.contains(resourceName)) {
            return hasCreateStatus(resourceDescription.get());
        }
        return false;
    }

    private Optional<CapabilityStatement.CapabilityStatementRestResourceComponent> getCapabilityResources(String resourceName) {
        return statement.getRest()
                .get(0)
                .getResource().stream()
                .filter(c -> resourceName.equals(c.getType()))
                .findFirst();
    }

    private boolean hasCreateStatus(
            CapabilityStatement.CapabilityStatementRestResourceComponent resourceDescription
    ) {
        Optional<CapabilityStatement.ResourceInteractionComponent> createStatus = resourceDescription.getInteraction()
                .stream()
                .filter(i -> "create".equals(i.getCode().getDisplay()))
                .findFirst();
        return createStatus.isPresent();
    }

}
