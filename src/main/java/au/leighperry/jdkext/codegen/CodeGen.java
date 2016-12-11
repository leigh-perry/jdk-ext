package au.leighperry.jdkext.codegen;

import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jooq.lambda.Seq.seq;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class CodeGen {
    private static final AtomicInteger objectId = new AtomicInteger();

    public static void resetObjectId() {
        objectId.set(0);
    }

    /**
     * Reflectively write java code to generate an instance of an object
     *
     * @param o the object of code generation
     * @return (initialisation-code, expression). For simple expressions initialisation-code is an
     * empty stream and expression contains the expression itself. For complex expressions initialisation-code
     * is a stream of instantiation code and expression contains the nested variable.
     */
    public static Tuple2<Seq<String>, String> getPreambleAndExpression(final Object o) {
        if (o == null) {
            return tuple(Seq.empty(), null);
        }

        final Class<?> type = o.getClass();
        final String typeName = type.getCanonicalName();

        if (type.isEnum()) {
            // return the object value as the expression
            final String s = String.format("%s.%s", typeName, o);
            return tuple(Seq.empty(), s);
        }

        if (isSimpleType(typeName, o)) {
            // return the object value as the expression
            if (Objects.equals(type, Long.class) || typeName.equals("long")) {
                return tuple(Seq.empty(), String.format("%sL", o));
            } else if (Objects.equals(type, String.class)) {
                return tuple(Seq.empty(), String.format("\"%s\"", o));
            } else if (Objects.equals(type, BigDecimal.class)) {
                return tuple(Seq.empty(), String.format("new java.math.BigDecimal(\"%s\")", ((BigDecimal) o).toPlainString()));
            } else {
                return tuple(Seq.empty(), o.toString());
            }
        }

        final String objectVariableName = getObjectVariableName(o);

        if (o instanceof List) {
            // Avoid private Arrays.asList()
            final String listTypeName = typeName.equals("java.util.Arrays.ArrayList") ? "java.util.ArrayList" : typeName;

            final String listInstantiation = String.format("    final List %s = new %s();", objectVariableName, listTypeName);
            return getCollectionPreambleAndExpression((List<?>) o, objectVariableName, listInstantiation);
        }

        if (o instanceof Set) {
            final String setInstantiation = String.format("    final Set %s = new %s();", objectVariableName, typeName);
            return getCollectionPreambleAndExpression((Set<?>) o, objectVariableName, setInstantiation);
        }

        if (o instanceof Map) {
            final Map<?, ?> mapObject = (Map<?, ?>) o;

            final String mapKeyInstantiation = String.format("    final Map %s = new %s();", objectVariableName, typeName);

            // Generate the code for each item in the map
            final Seq<String> mapKeyInitialisation =
                seq(mapObject)
                    .flatMap(
                        entry -> {
                            final Tuple2<Seq<String>, String> preambleAndExpressionKey = getPreambleAndExpression(entry.v1);
                            final Tuple2<Seq<String>, String> preambleAndExpressionValue = getPreambleAndExpression(entry.v2);

                            // preamble code for each mapKey item plus adding to the generated mapKey
                            return seq(preambleAndExpressionKey.v1)
                                .append(preambleAndExpressionValue.v1)
                                .append(
                                    String.format(
                                        "    %s.put(%s, %s);",
                                        objectVariableName,
                                        preambleAndExpressionKey.v2,
                                        preambleAndExpressionValue.v2
                                    )
                                );
                        }
                    )
                    .prepend(mapKeyInstantiation);

            return tuple(mapKeyInitialisation, objectVariableName);
        }

        // TODO other collections

        // Handle generic object fields via reflection
        final Seq<String> preamble =
            getProperties(o)
                .flatMap(
                    p -> {
                        // tuple is (field-name, value, type)
                        // preamble code for each object field plus setting the value via setter
                        final String fieldName = p.v1;
                        final Tuple2<Seq<String>, String> preambleAndExpression = getPreambleAndExpression(p.v2);

                        final String setter =
                            String.format(
                                "set%s%s",
                                fieldName.substring(0, 1).toUpperCase(),
                                fieldName.substring(1)
                            );

                        return seq(preambleAndExpression.v1)
                            .append(String.format("    %s.%s(%s);", objectVariableName, setter, preambleAndExpression.v2));
                    }
                )
                .prepend(String.format("    final %s %s = new %s();", typeName, objectVariableName, typeName));

        return tuple(preamble, objectVariableName);
    }

    private static Tuple2<Seq<String>, String> getCollectionPreambleAndExpression(
        final Iterable<?> collection,
        final String objectVariableName,
        final String listInstantiation
    ) {
        // Generate the code for each item in the list
        final Seq<String> listInitialisation =
            seq(collection)
                .map(CodeGen::getPreambleAndExpression)
                .flatMap(
                    // preamble code for each list item plus adding to the generated list
                    preambleAndExpression ->
                        seq(preambleAndExpression.v1)
                            .append(String.format("    %s.add(%s);", objectVariableName, preambleAndExpression.v2))
                )
                .prepend(listInstantiation);

        return tuple(listInitialisation, objectVariableName);
    }

    private static boolean isSimpleType(final String typeName, final Object value) {
        switch (typeName) {
            case "boolean":
            case "java.lang.Boolean":
            case "byte":
            case "java.lang.Byte":
            case "char":
            case "java.lang.Character":
            case "double":
            case "java.lang.Double":
            case "float":
            case "java.lang.Float":
            case "int":
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
            case "short":
            case "java.lang.Short":
            case "java.lang.String":
            case "java.math.BigDecimal":
                return true;
            default:
                return false;
        }
    }

    // Tuple is (field-name, value, type)
    private static Seq<? extends Tuple3<String, Object, ? extends Class<?>>> getProperties(final Object source) {
        try {
            final PropertyDescriptor[] sourceProperties = Introspector.getBeanInfo(source.getClass()).getPropertyDescriptors();

            return Seq.of(sourceProperties)
                .map(Unchecked.function(sp -> isReadWrite(source, sp)))
                .filter(Objects::nonNull);
        } catch (final IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    // Tuple is (field-name, value, type)
    private static Tuple3<String, Object, ? extends Class<?>> isReadWrite(
        final Object source,
        final PropertyDescriptor sp
    ) throws IllegalAccessException, InvocationTargetException {

        final Class<?> type = sp.getPropertyType();
        final String name = sp.getName();
        final Method writeMethod = sp.getWriteMethod();
        if (writeMethod == null) {
            return null;
        }

        final Method readMethod = sp.getReadMethod();
        if (readMethod == null) {
            return null;
        }

        final Object value = readMethod.invoke(source);
        return tuple(name, value, type);
    }

    private static String getObjectVariableName(final Object o) {
        return 'e' + o.getClass().getSimpleName() + objectId.getAndIncrement();
    }
}
