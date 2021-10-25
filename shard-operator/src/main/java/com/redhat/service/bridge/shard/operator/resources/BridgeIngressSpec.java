package com.redhat.service.bridge.shard.operator.resources;

/**
 * Since this will be a representation of a Deployment resource, ideally we should implement the Podspecable interface.
 * Supposed to be a Duck Type of Pod. SREs would need all the fine-tuning attributes possible in the target pod.
 * The Controller then can reconcile only the main fields that the core engine would care.
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 */
public class BridgeIngressSpec {

    private String image;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
