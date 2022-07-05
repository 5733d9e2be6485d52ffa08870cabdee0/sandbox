package com.redhat.service.smartevents.manager.api.models.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorFilterDefinitionException;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;

public class FiltersDeserializer extends StdDeserializer<Set<BaseFilter>> {

    private static class CapturingDeserializationContext extends DefaultDeserializationContext {

        private final DefaultDeserializationContext delegate;
        private final Consumer<String> invalidValueConsumer;

        public CapturingDeserializationContext(DefaultDeserializationContext delegate, Consumer<String> invalidValueConsumer) {
            super(delegate);
            this.delegate = delegate;
            this.invalidValueConsumer = invalidValueConsumer;
        }

        @Override
        public DefaultDeserializationContext with(DeserializerFactory factory) {
            return delegate.with(factory);
        }

        @Override
        public DefaultDeserializationContext createInstance(DeserializationConfig config, JsonParser p, InjectableValues values) {
            return delegate.createInstance(config, p, values);
        }

        @Override
        public DefaultDeserializationContext createDummyInstance(DeserializationConfig config) {
            return delegate.createDummyInstance(config);
        }

        @Override
        public JavaType handleUnknownTypeId(JavaType baseType, String id, TypeIdResolver idResolver, String extraDesc) throws IOException {
            invalidValueConsumer.accept(id);
            // Returning null forces the deserialization to ignore the value.
            // Required to prevent Jackson from throwing an Exception that it was unable to deserialise the value.
            return null;
        }

    }

    public FiltersDeserializer() {
        super((Class<?>) null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<BaseFilter> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Intercept default deserialization to capture errors
        List<String> invalidValues = new ArrayList<>();
        CapturingDeserializationContext cdc = new CapturingDeserializationContext((DefaultDeserializationContext) ctxt, invalidValues::add);
        CollectionType type = cdc.getTypeFactory().constructCollectionType(HashSet.class, BaseFilter.class);
        JsonDeserializer<Object> delegate = cdc.findRootValueDeserializer(type);
        Object deserializedValue = delegate.deserialize(p, cdc);

        if (!invalidValues.isEmpty()) {
            throw new ProcessorFilterDefinitionException(String.format("Invalid values:[%s]", String.join(", ", invalidValues)));
        }

        return (Set<BaseFilter>) deserializedValue;
    }

}
