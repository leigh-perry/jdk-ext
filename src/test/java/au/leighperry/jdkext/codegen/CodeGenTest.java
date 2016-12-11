package au.leighperry.jdkext.codegen;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
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
                        "    eArrayList0.add(au.leighperry.jdkext.codegen.CodeGenTest.TestEnum.A);" +
                        "    eArrayList0.add(au.leighperry.jdkext.codegen.CodeGenTest.TestEnum.B);" +
                        "    eArrayList0.add(au.leighperry.jdkext.codegen.CodeGenTest.TestEnum.C);",
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

        @Test
        public void getPreambleAndExpression_map_string() {
            final Map<String, String> map = new HashMap<>();
            map.put("a", "aa");
            map.put("b", "bb");
            map.put("c", "cc");

            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(map);
            assertAll(
                () -> assertEquals(
                    "    final Map eHashMap0 = new HashMap();" +
                        "    eHashMap0.put(\"a\", \"aa\");" +
                        "    eHashMap0.put(\"b\", \"bb\");" +
                        "    eHashMap0.put(\"c\", \"cc\");",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("eHashMap0", code.v2)
            );
        }

        @Test
        public void getPreambleAndExpression_map_objects() {
            // TreeMap to preserve ordering
            final Map<Pair<String, String>, Pair<String, String>> map = new TreeMap<>();
            map.put(new Pair<>("a", "aa"), new Pair<>("aaa", "aaaa"));
            map.put(new Pair<>("b", "bb"), new Pair<>("bbb", "bbbb"));
            map.put(new Pair<>("c", "cc"), new Pair<>("ccc", "cccc"));

            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(map);
            assertAll(
                () -> assertEquals(
                    "    final Map eTreeMap0 = new HashMap();" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair1 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair1.setT0(\"a\");" +
                        "    ePair1.setT1(\"aa\");" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair2 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair2.setT0(\"aaa\");" +
                        "    ePair2.setT1(\"aaaa\");" +
                        "    eTreeMap0.put(ePair1, ePair2);" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair3 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair3.setT0(\"b\");" +
                        "    ePair3.setT1(\"bb\");" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair4 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair4.setT0(\"bbb\");" +
                        "    ePair4.setT1(\"bbbb\");" +
                        "    eTreeMap0.put(ePair3, ePair4);" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair5 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair5.setT0(\"c\");" +
                        "    ePair5.setT1(\"cc\");" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair6 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    ePair6.setT0(\"ccc\");" +
                        "    ePair6.setT1(\"cccc\");" +
                        "    eTreeMap0.put(ePair5, ePair6);",
                    code.v1.collect(Collectors.joining())
                ),
                () -> assertEquals("eTreeMap0", code.v2)
            );
        }
    }

    static class Pair<T0 extends Comparable<T0>, T1 extends Comparable<T1>> implements Comparable<Pair<T0, T1>> {
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

        public final Comparator<Pair<T0, T1>> COMPARATOR =
            Comparator
                .comparing((Function<Pair<T0, T1>, T0>) Pair::getT0)
                .thenComparing(Pair::getT1);


        @Override
        public int compareTo(Pair<T0, T1> rhs) {
            return COMPARATOR.compare(this, rhs);
        }
    }

    @Nested
    @DisplayName("object expressions")
    class ObjectExpressions {
        @Test
        public void getPreambleAndExpression_object() {
            final Tuple2<Seq<String>, String> code = CodeGen.getPreambleAndExpression(new Pair<>(1234, "string0"));
            assertAll(
                () -> assertEquals(
                    "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair0 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
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
                    new Pair<>(new Pair<>(new Pair<>(1234, "string0"), "string1"), "string2")
                );
            assertAll(
                () -> assertEquals(
                    "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair0 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair1 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
                        "    final au.leighperry.jdkext.codegen.CodeGenTest.Pair ePair2 = new au.leighperry.jdkext.codegen.CodeGenTest.Pair();" +
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
