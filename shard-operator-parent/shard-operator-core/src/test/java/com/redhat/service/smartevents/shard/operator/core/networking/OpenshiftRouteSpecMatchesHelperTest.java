package com.redhat.service.smartevents.shard.operator.core.networking;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.openshift.api.model.RoutePort;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.RouteTargetReference;
import io.fabric8.openshift.api.model.TLSConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenshiftRouteSpecMatchesHelperTest {

    @ParameterizedTest
    @MethodSource("routeSpecParameters")
    void testParameters(RouteSpec existingSpec, RouteSpec expectedSpec,
            TLSConfig existingTLSConfig, TLSConfig expectedTLSConfig,
            String existingTLSCertificate, String expectedTLSCertificate,
            String existingTLSKey, String expectedTLSKey,
            RouteTargetReference existingTo, RouteTargetReference expectedTo,
            String existingName, String expectedName,
            RoutePort existingRoutePort, RoutePort expectedRoutePort,
            String existingPort, String expectedPort,
            boolean matches) {
        RouteSpec existing = buildRouteSpec(existingSpec,
                existingTLSConfig, existingTLSCertificate, existingTLSKey,
                existingTo,
                existingName,
                existingRoutePort,
                existingPort);
        RouteSpec expected = buildRouteSpec(expectedSpec,
                expectedTLSConfig, expectedTLSCertificate, expectedTLSKey,
                expectedTo,
                expectedName,
                expectedRoutePort,
                expectedPort);
        assertThat(OpenshiftRouteSpecMatchesHelper.matches(existing, expected)).isEqualTo(matches);
    }

    private RouteSpec buildRouteSpec(RouteSpec spec,
            TLSConfig tlsConfig,
            String tlsCertificate,
            String tlsKey,
            RouteTargetReference to,
            String name,
            RoutePort routePort,
            String port) {
        if (Objects.nonNull(spec)) {
            setUncheckedProperty(spec::setPath);
            if (Objects.nonNull(tlsConfig)) {
                spec.setTls(tlsConfig);
                setUncheckedProperty(tlsConfig::setCaCertificate);
                tlsConfig.setCertificate(tlsCertificate);
                tlsConfig.setKey(tlsKey);
            }
            if (Objects.nonNull(to)) {
                spec.setTo(to);
                setUncheckedProperty(to::setKind);
                to.setName(name);
            }
            if (Objects.nonNull(routePort)) {
                spec.setPort(routePort);
                setUncheckedProperty((v) -> routePort.setAdditionalProperty("unchecked", v));
                routePort.setTargetPort(new IntOrString(port));
            }
        }

        return spec;
    }

    // Set a unique value for unchecked properties as they should make no difference on the comparisons
    private void setUncheckedProperty(Consumer<String> setter) {
        setter.accept(UUID.randomUUID().toString());
    }

    private static Stream<Arguments> routeSpecParameters() {
        Object[][] arguments = {
                { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true },
                { new RouteSpec(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false },
                { null, new RouteSpec(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), null, null, null, null, null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, new TLSConfig(), null, null, null, null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), null, null, null, null, null, null, null, null, null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), "cert1", null, null, null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), null, "cert1", null, null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), "cert1", "cert1", null, null, null, null, null, null, null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), null, null, "key1", null, null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), null, null, null, "key1", null, null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), new TLSConfig(), new TLSConfig(), null, null, "key1", "key1", null, null, null, null, null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, new RouteTargetReference(), null, null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, new RouteTargetReference(), null, null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, new RouteTargetReference(), new RouteTargetReference(), null, null, null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, new RouteTargetReference(), new RouteTargetReference(), "name1", null, null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, new RouteTargetReference(), new RouteTargetReference(), null, "name1", null, null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, new RouteTargetReference(), new RouteTargetReference(), "name1", "name1", null, null, null, null, true },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, new RoutePort(), null, null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, null, new RoutePort(), null, null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, new RoutePort(), new RoutePort(), null, null, true },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, new RoutePort(), new RoutePort(), "port1", null, false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, new RoutePort(), new RoutePort(), null, "port1", false },
                { new RouteSpec(), new RouteSpec(), null, null, null, null, null, null, null, null, null, null, new RoutePort(), new RoutePort(), "port1", "port1", true }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
