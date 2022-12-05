package com.redhat.service.smartevents.shard.operator.core.networking;

import java.util.Objects;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.openshift.api.model.RouteSpec;

public class OpenshiftRouteSpecMatchesHelper {

    private OpenshiftRouteSpecMatchesHelper() {
        //Static utility class
    }

    public static boolean matches(RouteSpec existing, RouteSpec expected) {
        if (Objects.isNull(existing) && Objects.isNull(expected)) {
            return true;
        }
        if (Objects.nonNull(existing) && Objects.isNull(expected)) {
            return false;
        }
        if (Objects.isNull(existing)) {
            return false;
        }
        Function<RouteSpec, String> host = RouteSpec::getHost;
        if (!Objects.equals(host.apply(existing), host.apply(expected))) {
            return false;
        }

        if (Objects.nonNull(existing.getTls()) && Objects.isNull(expected.getTls())) {
            return false;
        }
        if (Objects.isNull(existing.getTls()) && Objects.nonNull(expected.getTls())) {
            return false;
        }
        if (Objects.nonNull(existing.getTls()) && Objects.nonNull(expected.getTls())) {
            Function<RouteSpec, String> tlsConfigCertificate = (RouteSpec source) -> source.getTls().getCertificate();
            if (!Objects.equals(tlsConfigCertificate.apply(existing), tlsConfigCertificate.apply(expected))) {
                return false;
            }
            Function<RouteSpec, String> tlsConfigKey = (RouteSpec source) -> source.getTls().getKey();
            if (!Objects.equals(tlsConfigKey.apply(existing), tlsConfigKey.apply(expected))) {
                return false;
            }
        }

        if (Objects.nonNull(existing.getTo()) && Objects.isNull(expected.getTo())) {
            return false;
        }
        if (Objects.isNull(existing.getTo()) && Objects.nonNull(expected.getTo())) {
            return false;
        }
        if (Objects.nonNull(existing.getTo()) && Objects.nonNull(expected.getTo())) {
            Function<RouteSpec, String> toName = (RouteSpec source) -> source.getTo().getName();
            if (!Objects.equals(toName.apply(existing), toName.apply(expected))) {
                return false;
            }
        }

        if (Objects.nonNull(existing.getPort()) && Objects.isNull(expected.getPort())) {
            return false;
        }
        if (Objects.isNull(existing.getPort()) && Objects.nonNull(expected.getPort())) {
            return false;
        }
        if (Objects.nonNull(existing.getPort()) && Objects.nonNull(expected.getPort())) {
            Function<RouteSpec, IntOrString> targetPort = (RouteSpec source) -> source.getPort().getTargetPort();
            if (!Objects.equals(targetPort.apply(existing), targetPort.apply(expected))) {
                return false;
            }
        }

        return true;
    }

}
