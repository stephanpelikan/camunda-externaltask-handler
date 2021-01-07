package org.camunda.bpm.externaltask.cdi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.PersistenceContext;

import org.apache.deltaspike.core.util.metadata.AnnotationInstanceProvider;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;

/**
 * @see https://www.informatik-aktuell.de/entwicklung/methoden/bean-testing-tdd-
 *      fuer-java-ee-anwendungen-ohne-mocks-und-application-server.html
 * @see https://github.com/NovaTecConsulting/BeanTest
 */
public class ResourceAwareBeanExtension implements Extension {

    private static final Inject INJECT = AnnotationInstanceProvider.of(Inject.class);

    public <X> void processInjectionTarget(@Observes @WithAnnotations({ Named.class }) ProcessAnnotatedType<X> pat) {
        if (pat.getAnnotatedType().isAnnotationPresent(Named.class)) {
            modifiyAnnotatedTypeMetadata(pat);
        }
    }

    private <X> void modifiyAnnotatedTypeMetadata(ProcessAnnotatedType<X> pat) {
        AnnotatedType<X> annotatedType = pat.getAnnotatedType();

        AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(annotatedType);

        // add inject annotation
        for (AnnotatedField<? super X> field : annotatedType.getFields()) {
            if (shouldInjectionAnnotationBeAddedToMember(field)) {
                builder.addToField(field, INJECT);
            }
        }
        for (AnnotatedMethod<? super X> method : annotatedType.getMethods()) {
            if (shouldInjectionAnnotationBeAddedToMember(method)) {
                builder.addToMethod(method, INJECT);
            }
        }

        // Set the wrapper instead the actual annotated type
        pat.setAnnotatedType(builder.create());

    }

    private <X> boolean shouldInjectionAnnotationBeAddedToMember(AnnotatedMember<? super X> member) {
        return !member.isAnnotationPresent(Inject.class) && hasJavaEEAnnotations(member);
    }

    private static final Set<Class<? extends Annotation>> JAVA_EE_ANNOTATIONS = createJavaEEAnnotationSet();

    private static Set<Class<? extends Annotation>> createJavaEEAnnotationSet() {
        Set<Class<? extends Annotation>> javaEEAnnotations = new HashSet<Class<? extends Annotation>>();
        javaEEAnnotations.add(Resource.class);
        javaEEAnnotations.add(EJB.class);
        javaEEAnnotations.add(PersistenceContext.class);
        return Collections.unmodifiableSet(javaEEAnnotations);
    }

    private <X> boolean hasJavaEEAnnotations(AnnotatedMember<? super X> member) {
        for (Class<? extends Annotation> javaEEannotation : JAVA_EE_ANNOTATIONS) {
            if (member.isAnnotationPresent(javaEEannotation)) {
                return true;
            }
        }
        return false;
    }

}
