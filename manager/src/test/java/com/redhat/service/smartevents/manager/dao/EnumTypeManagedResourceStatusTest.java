package com.redhat.service.smartevents.manager.dao;

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

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnumTypeManagedResourceStatusTest {

    @Mock
    TypeConfiguration typeConfiguration;

    @Mock
    JavaTypeDescriptorRegistry javaTypeDescriptorRegistry;

    @Mock
    @SuppressWarnings("raw")
    EnumJavaTypeDescriptor enumJavaTypeDescriptor;

    private EnumTypeManagedResourceStatus type;

    private enum TestEnum {
        ONE,
        TWO
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        this.type = new EnumTypeManagedResourceStatus();
        this.type.setTypeConfiguration(typeConfiguration);

        when(typeConfiguration.getJavaTypeDescriptorRegistry()).thenReturn(javaTypeDescriptorRegistry);
        when(javaTypeDescriptorRegistry.getDescriptor(any())).thenReturn(enumJavaTypeDescriptor);
    }

    @Test
    public void testSetParameterValuesOverrideWhenNull() {
        Properties parameters = null;

        type.setParameterValues(parameters);

        assertInstantiation();
    }

    @Test
    public void testSetParameterValuesOverrideWhenEmpty() {
        Properties parameters = new Properties();

        type.setParameterValues(parameters);

        assertInstantiation();
        assertThat(parameters.get(EnumType.ENUM)).isEqualTo(ManagedResourceStatus.class.getName());
        assertThat(parameters.get(EnumType.NAMED)).isEqualTo(Boolean.TRUE.toString());
    }

    @Test
    public void testSetParameterValuesOverrideWhenPrePopulated() {
        Properties parameters = new Properties();
        parameters.setProperty(EnumType.ENUM, TestEnum.class.getName());
        parameters.setProperty(EnumType.NAMED, Boolean.FALSE.toString());

        type.setParameterValues(parameters);

        assertThat(parameters.get(EnumType.ENUM)).isEqualTo(TestEnum.class.getName());
        assertThat(parameters.get(EnumType.NAMED)).isEqualTo(Boolean.FALSE.toString());
    }

    private void assertInstantiation() {
        assertThat(type.isOrdinal()).isFalse();
        assertThat(type.returnedClass()).isEqualTo(ManagedResourceStatus.class);
    }
}
