package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

public class KafkaSASLSpec {

    boolean enable;

    SecretValueFromSource user;

    SecretValueFromSource password;

    SecretValueFromSource type;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public SecretValueFromSource getUser() {
        return user;
    }

    public void setUser(SecretValueFromSource user) {
        this.user = user;
    }

    public SecretValueFromSource getPassword() {
        return password;
    }

    public void setPassword(SecretValueFromSource password) {
        this.password = password;
    }

    public SecretValueFromSource getType() {
        return type;
    }

    public void setType(SecretValueFromSource type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KafkaSASLSpec that = (KafkaSASLSpec) o;
        return enable == that.enable && Objects.equals(user, that.user) && Objects.equals(password, that.password) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enable, user, password, type);
    }
}
