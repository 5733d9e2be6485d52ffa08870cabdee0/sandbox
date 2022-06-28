package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.models.CloudProvider;
import com.redhat.service.smartevents.manager.models.CloudRegion;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CloudProviderConstraintValidatorTest {

    @Inject
    CloudProviderConstraintValidator validator;

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Inject
    ValidatorFactory validatorFactory;

    @Test
    public void validate_invalidCloudProvider() {

        String invalidCloudProvider = "dodgyProvider";
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, "dodgyProvider", region.getName());

        validateConstraintMessage(br, "Cloud Provider '" + invalidCloudProvider + "' is not valid.");
    }

    @Test
    public void validate_invalidCloudRegion() {
        String invalidRegion = "dodgyRegion";
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, invalidRegion);

        validateConstraintMessage(br, "The Region '" + invalidRegion + "' for Cloud Provider '" + DEFAULT_CLOUD_PROVIDER + "' is not valid.");
    }

    @Test
    public void validate_cloudProviderNotEnabled() {

        String disabledProviderId = "gcp";
        CloudProvider cp = cloudProviderDAO.findById(disabledProviderId);
        assertThat(cp.isEnabled()).isFalse();

        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, cp.getId(), region.getName());

        validateConstraintMessage(br, "Cloud Provider '" + disabledProviderId + "' is not enabled.");
    }

    @Test
    public void validate_regionNotEnabled() {

        String disabledRegionName = "eu-west-1";
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegionByName(disabledRegionName).get();
        assertThat(region.isEnabled()).isFalse();

        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, region.getName());

        validateConstraintMessage(br, "The Region '" + disabledRegionName + "' for Cloud Provider '" + DEFAULT_CLOUD_PROVIDER + "' is not enabled.");
    }

    private void validateConstraintMessage(BridgeRequest br, String expectedMessage) {
        Set<ConstraintViolation<BridgeRequest>> violations = validatorFactory.getValidator().validate(br);
        assertThat(violations).hasSize(1);

        ConstraintViolation<BridgeRequest> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo(expectedMessage);
    }
}
