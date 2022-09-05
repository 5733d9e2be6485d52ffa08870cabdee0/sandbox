package com.redhat.service.smartevents.shard.operator.networking;

import java.util.Objects;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.openshift.api.model.RouteSpec;

public class OpenshiftRouteSpecMatchesHelper {

    private OpenshiftRouteSpecMatchesHelper() {
        //Static utility class
    }

    public static boolean matches(RouteSpec existing, RouteSpec expected) {
        Function<RouteSpec, RouteSpec> spec = (RouteSpec source) -> source;
        if (!Objects.equals(spec.apply(existing), spec.apply(expected))) {
            return false;
        }

        Function<RouteSpec, String> host = (RouteSpec source) -> {
            if (Objects.isNull(source)) {
                return null;
            }
            return source.getHost();
        };
        if (!Objects.equals(host.apply(existing), host.apply(expected))) {
            return false;
        }

        Function<RouteSpec, String> tlsConfigCertificate = (RouteSpec source) -> {
            if (Objects.isNull(source)) {
                return null;
            }
            if (Objects.isNull(source.getTls())) {
                return null;
            }
            return source.getTls().getCertificate();
        };
        if (!Objects.equals(tlsConfigCertificate.apply(existing), tlsConfigCertificate.apply(expected))) {
            return false;
        }

        Function<RouteSpec, String> tlsConfigKey = (RouteSpec source) -> {
            if (Objects.isNull(source)) {
                return null;
            }
            if (Objects.isNull(source.getTls())) {
                return null;
            }
            return source.getTls().getKey();
        };
        if (!Objects.equals(tlsConfigKey.apply(existing), tlsConfigKey.apply(expected))) {
            return false;
        }

        Function<RouteSpec, String> toName = (RouteSpec source) -> {
            if (Objects.isNull(source)) {
                return null;
            }
            if (Objects.isNull(source.getTo())) {
                return null;
            }
            return source.getTo().getName();
        };
        if (!Objects.equals(toName.apply(existing), toName.apply(expected))) {
            return false;
        }

        Function<RouteSpec, IntOrString> targetPort = (RouteSpec source) -> {
            if (Objects.isNull(source)) {
                return null;
            }
            if (Objects.isNull(source.getPort())) {
                return null;
            }
            return source.getPort().getTargetPort();
        };
        if (!Objects.equals(targetPort.apply(existing), targetPort.apply(expected))) {
            return false;
        }

        return true;
    }

}
