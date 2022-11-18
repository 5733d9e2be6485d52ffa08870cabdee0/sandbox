package com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1beta1")
@Group("security.istio.io")
@Kind("RequestAuthentication")
public class RequestAuthentication extends CustomResource<RequestAuthenticationSpec, Void> implements Namespaced {
}
