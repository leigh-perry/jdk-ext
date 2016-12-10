package au.leighperry.jdkext.codegen;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CodeGenTest {
    @BeforeEach
    void reset() {
        CodeGen.resetObjectId();
    }

    @Nested
    @DisplayName("simple expressions")
    class SimpleExpressions {
        @Test
        public void getPreambleAndExpression_simple_int() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(3333335);
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("3333335", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_simple_Integer() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(Integer.valueOf(3333335));
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("3333335", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_simple_long() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(444444555555666635L);
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("444444555555666635L", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_simple_Long() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(Long.valueOf(444444555555666635L));
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("444444555555666635L", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_string() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression("asdfasdf");
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("\"asdfasdf\"", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_BigDecimal() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(BigDecimal.valueOf(123412341234L));
            assertAll(
                () -> assertTrue(code.v1.isEmpty()),
                () -> assertEquals("new java.math.BigDecimal(\"123412341234\")", code.v2)
            );
        }
    }

    enum TestEnum {A, B, C}

    @Nested
    @DisplayName("collection expressions")
    class CollectionExpressions {
        @Test
        public void getPreambleAndExpression_list_string() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(Arrays.asList("a", "b", "c"));
            assertAll(
                () -> assertEquals(
                    "    final List eArrayList0 = new ArrayList();" +
                        "    eArrayList0.add(\"a\");" +
                        "    eArrayList0.add(\"b\");" +
                        "    eArrayList0.add(\"c\");",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("eArrayList0", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_list_enum() {
            final Tuple2<Seq<String>, String> code =
                CodeGen.getPreambleAndExpression(
                    Arrays.asList(
                        TestEnum.A,
                        TestEnum.B,
                        TestEnum.C
                    )
                );
            assertAll(
                () -> assertEquals(
                    "    final List eArrayList0 = new ArrayList();" +
                        "    eArrayList0.add(test.CodeGenTest.TestEnum.A);" +
                        "    eArrayList0.add(test.CodeGenTest.TestEnum.B);" +
                        "    eArrayList0.add(test.CodeGenTest.TestEnum.C);",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("eArrayList0", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_list_list() {
            final Tuple2<Seq<String>, String> code =
                CodeGen.getPreambleAndExpression(
                    Arrays.asList(
                        Arrays.asList("a", "b", "c"),
                        Arrays.asList("d", "e", "f")
                    ));
            assertAll(
                () -> assertEquals(
                    "    final List eArrayList0 = new ArrayList();" +
                        "    final List eArrayList1 = new ArrayList();" +
                        "    eArrayList1.add(\"a\");" +
                        "    eArrayList1.add(\"b\");" +
                        "    eArrayList1.add(\"c\");" +
                        "    eArrayList0.add(eArrayList1);" +
                        "    final List eArrayList2 = new ArrayList();" +
                        "    eArrayList2.add(\"d\");" +
                        "    eArrayList2.add(\"e\");" +
                        "    eArrayList2.add(\"f\");" +
                        "    eArrayList0.add(eArrayList2);",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("eArrayList0", code.v2)
            );
        }
    }

    static class Pair<T0, T1> {
        T0 t0;
        T1 t1;

        public T0 getT0() {
            return t0;
        }

        public void setT0(final T0 t0) {
            this.t0 = t0;
        }

        public T1 getT1() {
            return t1;
        }

        public void setT1(final T1 t1) {
            this.t1 = t1;
        }

        public Pair(final T0 t0, final T1 t1) {
            this.t0 = t0;
            this.t1 = t1;
        }

        public static <T0, T1> Pair<T0, T1> of(final T0 e0, final T1 e1) {
            return new Pair<>(e0, e1);
        }
    }

    @Nested
    @DisplayName("object expressions")
    class ObjectExpressions {
        @Test
        public void getPreambleAndExpression_object() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(Pair.of(1234, "string0"));
            assertAll(
                () -> assertEquals(
                    "    final test.CodeGenTest.Pair ePair0 = new test.CodeGenTest.Pair();" +
                        "    ePair0.setT0(1234);" +
                        "    ePair0.setT1(\"string0\");",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("ePair0", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_object_nested() {
            final Tuple2<Seq<String>, String> code =
                CodeGen.getPreambleAndExpression(
                    Pair.of(
                        Pair.of(
                            Pair.of(1234, "string0"),
                            "string1"
                        ),
                        "string2"
                    )
                );
            assertAll(
                () -> assertEquals(
                    "    final test.CodeGenTest.Pair ePair0 = new test.CodeGenTest.Pair();" +
                        "    final test.CodeGenTest.Pair ePair1 = new test.CodeGenTest.Pair();" +
                        "    final test.CodeGenTest.Pair ePair2 = new test.CodeGenTest.Pair();" +
                        "    ePair2.setT0(1234);" +
                        "    ePair2.setT1(\"string0\");" +
                        "    ePair1.setT0(ePair2);" +
                        "    ePair1.setT1(\"string1\");" +
                        "    ePair0.setT0(ePair1);" +
                        "    ePair0.setT1(\"string2\");",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("ePair0", code.v2)
            );

        }
    }
}
