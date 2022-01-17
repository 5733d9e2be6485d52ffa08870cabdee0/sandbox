package com.redhat.service.bridge.shard.operator;

import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SanitizeNameTest {

    @Test
    public void test(){
        String test = KubernetesResourceUtil.sanitizeName("myId");
        assertThat(test).isEqualTo("myid");
    }
}
