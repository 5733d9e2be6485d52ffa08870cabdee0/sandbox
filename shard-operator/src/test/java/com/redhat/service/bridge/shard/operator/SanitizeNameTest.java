package com.redhat.service.bridge.shard.operator;

import org.junit.Test;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class SanitizeNameTest {

    @Test
    public void test() {
        String test = KubernetesResourceUtil.sanitizeName("myId");
        assertThat(test).isEqualTo("myid");
    }
}
