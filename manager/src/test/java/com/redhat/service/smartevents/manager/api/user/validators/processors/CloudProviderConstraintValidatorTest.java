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
    CloudProviderDAO cloudProviderDAO;

    @Inject
    ValidatorFactory validatorFactory;

    @Test
    public void validate_nullCloudProvider() {
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, null, region.getName());

        Set<String> violations = Set.of(
                "Cloud Provider cannot be null or empty.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_emptyCloudProvider() {
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, "", region.getName());

        Set<String> violations = Set.of(
                "Cloud Provider cannot be null or empty.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_nullRegion() {
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, cp.getId(), null);

        Set<String> violations = Set.of(
                "Cloud Region cannot be null or empty.",
                "The supplied Cloud Provider details are not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_invalidCloudProvider() {

        String invalidCloudProvider = "dodgyProvider";
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, "dodgyProvider", region.getName());

        Set<String> violations = Set.of(
                "The requested Cloud Provider '" + invalidCloudProvider + "' is not valid.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_invalidProviderAndRegion() {
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, "foo", "bar");
        Set<String> violations = Set.of(
                "The requested Cloud Provider '" + br.getCloudProvider() + "' is not valid.",
                "The requested Region '" + br.getRegion() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_emptyProviderAndRegion() {

        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, "", "");

        Set<String> violations = Set.of(
                "Cloud Region cannot be null or empty.",
                "Cloud Provider cannot be null or empty.",
                "The supplied Cloud Provider details are not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_nullProviderAndNullRegion() {
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, null, null);

        Set<String> violations = Set.of(
                "Cloud Region cannot be null or empty.",
                "Cloud Provider cannot be null or empty.",
                "The supplied Cloud Provider details are not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_invalidCloudRegion() {
        String invalidRegion = "dodgyRegion";
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, invalidRegion);

        Set<String> violations = Set.of(
                "The requested Region '" + invalidRegion + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_cloudProviderNotEnabled() {

        String disabledProviderId = "gcp";
        CloudProvider cp = cloudProviderDAO.findById(disabledProviderId);
        assertThat(cp.isEnabled()).isFalse();

        CloudRegion region = cp.getRegions().get(0);
        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, cp.getId(), region.getName());

        Set<String> violations = Set.of(
                "The requested Cloud Provider '" + disabledProviderId + "' is not enabled.",
                "The requested Region '" + region.getName() + "' is not enabled.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_regionNotEnabled() {

        String disabledRegionName = "eu-west-1";
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegionByName(disabledRegionName).get();
        assertThat(region.isEnabled()).isFalse();

        BridgeRequest br = new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, region.getName());
        Set<String> violations = Set.of(
                "The requested Region '" + disabledRegionName + "' is not enabled.");

        validateConstraintMessage(br, violations);
    }

    private void validateConstraintMessage(BridgeRequest br, Set<String> expectedMessages) {
        Set<ConstraintViolation<BridgeRequest>> violations = validatorFactory.getValidator().validate(br);
        assertThat(violations)
                .hasSize(expectedMessages.size())
                .allSatisfy((v) -> expectedMessages.contains(v.getMessage()));
    }
}
