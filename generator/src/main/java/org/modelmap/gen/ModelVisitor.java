package org.modelmap.gen;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.beans.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;

import org.apache.maven.plugin.logging.Log;
import org.modelmap.core.*;

final class ModelVisitor {

    private final Log log;

    public ModelVisitor(Log log) {
        this.log = log;
    }

    public void visitModel(Class<?> clazz, Visitor visitor, String packageFilter)
                    throws IntrospectionException, IllegalArgumentException, IllegalAccessException,
                    InvocationTargetException {
        log.debug("starting visiting class " + clazz.getName());
        visitModel(clazz, visitor, new LinkedList<>(), packageFilter, 0);
    }

    private void visitModel(Class<?> clazz, Visitor visitor, LinkedList<Method> path, String packageFilter, int deep)
                    throws IntrospectionException, IllegalArgumentException                     {
        if (clazz == null)
            return;
        if (clazz.getPackage() == null)
            return;
        if (!clazz.getPackage().getName().startsWith(packageFilter))
            return;
        if (clazz.isEnum())
            return;
        if (deep > 8)
            return;

        log.debug("class " + clazz.getName());
        final BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor desc : propertyDescriptors) {
            log.debug("property " + desc.getName() + " : " + desc.getPropertyType().getSimpleName()
                            + " from " + clazz.getName());
            path.addLast(desc.getReadMethod());
            try {
                final Map<FieldId, PathConstraint> formParam = getFieldTarget(clazz, desc);
                if (formParam.isEmpty()) {
                    continue;
                }
                log.debug(formParam.size() + " path(s) found from  " + desc);
                visitor.visit(formParam, desc.getReadMethod(), desc.getWriteMethod(), path);
            } finally {
                path.removeLast();
            }
        }
        final Collection<Method> otherMethods = methods(clazz, packageFilter);
        for (Method method : otherMethods) {
            path.addLast(method);
            try {
                visitModel(returnType(method, packageFilter), visitor, path, packageFilter, deep + 1);
            } finally {
                path.removeLast();
            }
        }
        visitModel(clazz.getSuperclass(), visitor, path, packageFilter, deep + 1);
    }

    private Map<FieldId, PathConstraint> getFieldTarget(Class<?> clazz, PropertyDescriptor desc) {
        if (desc.getReadMethod() == null || desc.getWriteMethod() == null) {
            return emptyMap();
        }
        Map<FieldId, PathConstraint> fieldTarget = emptyMap();
        try {
            Field field = clazz.getDeclaredField(desc.getName());
            fieldTarget = getFieldTarget(field);
        } catch (NoSuchFieldException e) {
            // derived field without declared field
        }
        if (fieldTarget.isEmpty()) {
            fieldTarget = getFieldTarget(desc.getReadMethod());
        }
        if (fieldTarget.isEmpty()) {
            fieldTarget = getFieldTarget(desc.getWriteMethod());
        }
        return fieldTarget;
    }

    private Map<FieldId, PathConstraint> getFieldTarget(AccessibleObject executable) {
        final Annotation[] annotations = executable.getAnnotations();
        log.debug(annotations.length + " annotations to process from " + executable.toString());

        // retrive declared path annotations type
        Set<Class<? extends Annotation>> pathAnnotations = stream(annotations)
                        .filter(a -> a.annotationType().getAnnotation(Path.class) != null)
                        .map(Annotation::annotationType)
                        .collect(toSet());

        // add those from repeatable annotations
        stream(annotations).forEach(a -> {
            try {
                Method value = a.annotationType().getMethod("value");
                Class<?> returnType = value.getReturnType();
                if (returnType.isArray() && returnType.getComponentType().isAnnotation()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> type = (Class<? extends Annotation>) returnType.getComponentType();
                    pathAnnotations.add(type);
                }
            } catch (NoSuchMethodException e) {
                // no a repeatable annotations, skipping
            }
        });

        log.debug(pathAnnotations.size() + " paths annotations to process from " + executable.toString());
        return pathAnnotations.stream()
                        .map(a -> asList(executable.getAnnotationsByType(a)))
                        .flatMap(Collection::stream)
                        .map(a -> {
                            try {
                                log.debug("process annotation " + a.toString());
                                Method fieldIdGetter = getMethodByClass(a.annotationType(), FieldId.class);
                                Method contraintGetter = getMethodByClass(a.annotationType(), PathConstraint.class);
                                return new SimpleImmutableEntry<>((FieldId) fieldIdGetter.invoke(a),
                                                (PathConstraint) contraintGetter.invoke(a));
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
    }

    private Method getMethodByClass(Class<?> clazz, Class<?> interfaceType) {
        log.debug("process annotation type " + clazz.getName());
        return stream(clazz.getMethods())
                        .filter(f -> {
                            log.debug("process annotation field " + f.toString());
                            return asList(f.getReturnType().getInterfaces()).contains(interfaceType);
                        })
                        .findFirst().get();
    }

    private Collection<Method> methods(Class<?> clazz, String packageFilter) {
        if (clazz == null) {
            return emptySet();
        }
        if (clazz.getPackage() == null) {
            return emptySet();
        }
        if (!clazz.getPackage().getName().startsWith(packageFilter)) {
            return emptySet();
        }
        return filter(clazz.getMethods(), packageFilter);
    }

    private Collection<Method> filter(Method[] methods, String packageFilter) {
        final List<Method> filtered = stream(methods)
                        .filter(m -> !Modifier.isStatic(m.getModifiers()))
                        .filter(m -> !Modifier.isNative(m.getModifiers()))
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> m.getReturnType() != null)
                        .filter(m -> !m.getReturnType().equals(Void.TYPE))
                        .filter(m -> m.getParameterTypes().length == 0)
                        .filter(m -> m.getDeclaringClass().getPackage().getName().startsWith(packageFilter))
                        .filter(m -> !m.getName().toLowerCase().contains("clone"))
                        .collect(toList());

        final List<Method> overrided = new ArrayList<>();
        for (Method method : filtered) {
            for (Method method2 : filtered) {
                if (method == method2) {
                    continue;
                }
                if (!method.getName().equals(method2.getName())) {
                    continue;
                }
                if (method.getReturnType().isAssignableFrom(method2.getReturnType())) {
                    overrided.add(method);
                }
            }
        }
        filtered.removeAll(overrided);
        return filtered;
    }

    private Class<?> returnType(Method method, String packageFilter) {
        if (method.getGenericReturnType() == null) {
            return method.getReturnType();
        }
        if (method.getGenericReturnType().getTypeName().equals(method.getReturnType().getTypeName())) {
            return method.getReturnType();
        }
        if (method.getGenericReturnType() instanceof Class) {
            return (Class<?>) method.getGenericReturnType();
        }
        if (List.class.isAssignableFrom(method.getReturnType())
                        && method.getGenericReturnType() instanceof ParameterizedType) {
            final ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
            if (returnType == null) {
                return null;
            }
            if (returnType.getActualTypeArguments() == null) {
                return null;
            }
            if (returnType.getActualTypeArguments().length != 1) {
                return null;
            }
            if (returnType.getActualTypeArguments()[0].toString().startsWith(packageFilter)) {
                return null;
            }
            return (Class<?>) returnType.getActualTypeArguments()[0];
        }
        return method.getReturnType();
    }
}
