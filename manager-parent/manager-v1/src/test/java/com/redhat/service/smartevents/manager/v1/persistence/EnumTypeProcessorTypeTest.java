package com.redhat.service.smartevents.manager.v1.persistence;

import java.util.Properties;

import org.hibernate.type.EnumType;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnumTypeProcessorTypeTest {

    @Mock
    TypeConfiguration typeConfiguration;

    @Mock
    JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;

    @Mock
    @SuppressWarnings("raw")
    EnumJavaTypeDescriptor enumJavaTypeDescriptor;

    private EnumTypeProcessorType type;

    private enum TestEnum {
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        this.type = new EnumTypeProcessorType();
        this.type.setTypeConfiguration(typeConfiguration);

        when(typeConfiguration.getJavaTypeDescriptorRegistry()).thenReturn(javaTypeDescriptorRegistry);
        when(javaTypeDescriptorRegistry.getDescriptor(any())).thenReturn(enumJavaTypeDescriptor);
    }

    @Test
    public void testSetParameterValuesOverrideWhenNull() {
        type.setParameterValues(null);

        assertDefaultInstantiation();
    }

    @Test
    public void testSetParameterValuesOverrideWhenEmpty() {
        Properties parameters = new Properties();

        type.setParameterValues(parameters);

        assertDefaultInstantiation();
        assertThat(parameters.isEmpty()).isTrue();
    }

    @Test
    public void testSetParameterValuesOverrideWhenPrePopulated() {
        Properties parameters = new Properties();
        parameters.setProperty(EnumType.ENUM, TestEnum.class.getName());
        parameters.setProperty(EnumType.NAMED, Boolean.FALSE.toString());

        type.setParameterValues(parameters);

        // When the properties are already set they take precedence over our workaround
        assertThat(parameters.get(EnumType.ENUM)).isEqualTo(TestEnum.class.getName());
        assertThat(parameters.get(EnumType.NAMED)).isEqualTo(Boolean.FALSE.toString());
    }

    private void assertDefaultInstantiation() {
        assertThat(type.isOrdinal()).isFalse();
        assertThat(type.returnedClass()).isEqualTo(ProcessorType.class);
    }

}
