package com.redhat.service.smartevents.manager.v1.api.user.validators.processors;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.persistence.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequestV1;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v1.TestConstants.DEFAULT_CLOUD_PROVIDER;
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
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, null, region.getName());

        Set<String> violations = Set.of(
                "Cloud Provider cannot be null or empty.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_emptyCloudProvider() {
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        CloudRegion region = cp.getRegions().get(0);
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, "", region.getName());

        Set<String> violations = Set.of(
                "Cloud Provider cannot be null or empty.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_nullRegion() {
        CloudProvider cp = cloudProviderDAO.findById(DEFAULT_CLOUD_PROVIDER);
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, cp.getId(), null);

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
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, "dodgyProvider", region.getName());

        Set<String> violations = Set.of(
                "The requested Cloud Provider '" + invalidCloudProvider + "' is not valid.",
                "The requested Region '" + region.getName() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_invalidProviderAndRegion() {
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, "foo", "bar");
        Set<String> violations = Set.of(
                "The requested Cloud Provider '" + br.getCloudProvider() + "' is not valid.",
                "The requested Region '" + br.getRegion() + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_emptyProviderAndRegion() {

        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, "", "");

        Set<String> violations = Set.of(
                "Cloud Region cannot be null or empty.",
                "Cloud Provider cannot be null or empty.",
                "The supplied Cloud Provider details are not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_nullProviderAndNullRegion() {
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, null, null);

        Set<String> violations = Set.of(
                "Cloud Region cannot be null or empty.",
                "Cloud Provider cannot be null or empty.",
                "The supplied Cloud Provider details are not valid.");

        validateConstraintMessage(br, violations);
    }

    @Test
    public void validate_invalidCloudRegion() {
        String invalidRegion = "dodgyRegion";
        BridgeRequestV1 br = new BridgeRequestV1(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, invalidRegion);

        Set<String> violations = Set.of(
                "The requested Region '" + invalidRegion + "' is not valid.");

        validateConstraintMessage(br, violations);
    }

    private void validateConstraintMessage(BridgeRequestV1 br, Set<String> expectedMessages) {
        Set<ConstraintViolation<BridgeRequestV1>> violations = validatorFactory.getValidator().validate(br);
        assertThat(violations)
                .hasSize(expectedMessages.size())
                .allSatisfy((v) -> expectedMessages.contains(v.getMessage()));
    }
}
