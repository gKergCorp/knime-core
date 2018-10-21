/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */

package org.knime.core.data.convert;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.BooleanToDataCellConverter;
import org.knime.core.data.convert.datacell.DoubleToDataCellConverter;
import org.knime.core.data.convert.datacell.IntToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.datacell.LongToDataCellConverter;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Test for all {@link JavaToDataCellConverterFactory}s and {@link JavaToDataCellConverterRegistry}.
 *
 * Naming of method follows: <code> testTo[DestType]() {
 * // all tests from any source type to DestType
 * } </code>
 *
 * @author Jonathan Hale
 * @see JavaToDataCellConverterRegistry
 * @see JavaToDataCellConverterFactory
 */
public class JavaToDataCellConversionTest {

    private static final double FUZZY_DOUBLE_TOLERANCE = 0.000001;

    /**
     * Test {@link ConversionKey#equals(Object)}.
     */
    @Test
    public void testKeysEqual() {
        ConversionKey key = new ConversionKey(Boolean.class, BooleanCell.TYPE);
        ConversionKey key2 = new ConversionKey(Boolean.class, BooleanCell.TYPE);

        assertEquals(key, key2);
    }

    /**
     * Generic test for simple {@link JavaToDataCellConverterFactory}s.
     *
     * @param sourceType type of the input to convert
     * @param dataType {@link DataType} of the {@link DataCell} to convert to.
     * @param destType type of the expected output {@link DataCell} subtype.
     * @param sourceValue a value to convert.
     * @return <code>sourceValue</code> converted to <code>destType</code>.
     * @throws Exception
     */
    protected <S, D> D testSimpleConversion(final Class<S> sourceType, final DataType dataType, final Class<D> destType,
        final S sourceValue) throws Exception {
        final JavaToDataCellConverter<S> converter = getConverter(sourceType, dataType);
        final DataCell converted = converter.convert(sourceValue);
        assertTrue(destType.isInstance(converted));
        @SuppressWarnings("unchecked") // checked in the above assert.
        D d = (D)converted;
        return d;
    }

    /**
     * Returns the first converter that is found for the combination of source type and destination type. Tests that a
     * corresponding converter factory is present and that it creates a non-{@code null} converter.
     *
     * @param sourceType Source type.
     * @param destDataType {@link DataType} of the {@link DataCell} into which to convert.
     * @return The first found converter, not {@code null}.
     */
    protected <S> JavaToDataCellConverter<S> getConverter(final Class<S> sourceType, final DataType destDataType) {
        final Optional<JavaToDataCellConverterFactory<S>> factory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(sourceType, destDataType).stream().findFirst();
        assertTrue(factory.isPresent());
        final JavaToDataCellConverter<S> converter = factory.get().create(null);
        assertNotNull(converter);
        return converter;
    }

    /**
     * Test Boolean -> BooleanCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToBooleanCell() throws Exception {
        final BooleanCell cell =
            testSimpleConversion(Boolean.class, BooleanCell.TYPE, BooleanCell.class, new Boolean(true));
        assertTrue(cell.getBooleanValue());
    }

    /**
     * Test boolean -> BooleanCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testPrimitiveToBooleanCell() throws Exception {
        final JavaToDataCellConverter<Boolean> converter = getConverter(boolean.class, BooleanCell.TYPE);
        assertThat(converter, instanceOf(BooleanToDataCellConverter.class));
        final BooleanToDataCellConverter primitiveConverter = (BooleanToDataCellConverter)converter;
        final BooleanCell cell = (BooleanCell)primitiveConverter.convertBoolean(true);
        assertEquals(true, cell.getBooleanValue());
    }

    /**
     * Test Integer -> IntCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToIntCell() throws Exception {
        final IntCell cell = testSimpleConversion(Integer.class, IntCell.TYPE, IntCell.class, new Integer(42));
        assertEquals(42, cell.getIntValue());
    }

    /**
     * Test int -> IntCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testPrimitiveToIntCell() throws Exception {
        final JavaToDataCellConverter<Integer> converter = getConverter(int.class, IntCell.TYPE);
        assertThat(converter, instanceOf(IntToDataCellConverter.class));
        final IntToDataCellConverter primitiveConverter = (IntToDataCellConverter)converter;
        final IntCell cell = (IntCell)primitiveConverter.convertInt(42);
        assertEquals(42, cell.getIntValue());
    }

    /**
     * Test Long -> LongCell and Integer -> LongCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToLongCell() throws Exception {
        final LongCell cell = testSimpleConversion(Long.class, LongCell.TYPE, LongCell.class, new Long(42L));
        assertEquals(42L, cell.getLongValue());

        final LongCell cell1 = testSimpleConversion(Integer.class, LongCell.TYPE, LongCell.class, new Integer(412));
        assertEquals(412L, cell1.getLongValue());
    }

    /**
     * Test long -> LongCell and int -> LongCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testPrimitiveToLongCell() throws Exception {
        final JavaToDataCellConverter<Long> longConverter = getConverter(long.class, LongCell.TYPE);
        assertThat(longConverter, instanceOf(LongToDataCellConverter.class));
        final LongToDataCellConverter primitiveLongConverter = (LongToDataCellConverter)longConverter;
        final LongCell cellFromLong = (LongCell)primitiveLongConverter.convertLong(42l);
        assertEquals(42l, cellFromLong.getLongValue());

        final JavaToDataCellConverter<Integer> integerConverter = getConverter(int.class, LongCell.TYPE);
        assertThat(integerConverter, instanceOf(IntToDataCellConverter.class));
        final IntToDataCellConverter primitiveIntConverter = (IntToDataCellConverter)integerConverter;
        final LongCell cellFromInt = (LongCell)primitiveIntConverter.convertInt(42);
        assertEquals(42l, cellFromInt.getLongValue());
    }

    /**
     * Test Double -> DoubleCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToDoubleCell() throws Exception {
        final DoubleCell cell =
            testSimpleConversion(Double.class, DoubleCell.TYPE, DoubleCell.class, new Double(Math.PI));
        assertEquals(Math.PI, cell.getDoubleValue(), FUZZY_DOUBLE_TOLERANCE);
    }

    /**
     * Test double -> DoubleCell conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testPrimitiveToDoubleCell() throws Exception {
        final JavaToDataCellConverter<Double> converter = getConverter(double.class, DoubleCell.TYPE);
        assertThat(converter, instanceOf(DoubleToDataCellConverter.class));
        final DoubleToDataCellConverter primitiveConverter = (DoubleToDataCellConverter)converter;
        final DoubleCell cell = (DoubleCell)primitiveConverter.convertDouble(Math.PI);
        assertEquals(Math.PI, cell.getDoubleValue(), FUZZY_DOUBLE_TOLERANCE);
    }

    /**
     * Test String -> StringCell and Object -> StringCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToStringCell() throws Exception {
        final StringCell cell =
            testSimpleConversion(String.class, StringCell.TYPE, StringCell.class, new String("KNIME"));
        assertEquals("KNIME", cell.getStringValue());
    }

    /**
     * Test String -> XMLCell, Document -> XMLCell and InputStream -> XMLCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToXMLCell() throws Exception {
        final String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KNIME>\n</KNIME>";

        /* from String */
        {
            final XMLCell xmlCell =
                testSimpleConversion(String.class, XMLCell.TYPE, XMLCell.class, new String("<KNIME />"));
            assertEquals(xmlString, xmlCell.getStringValue().replace('\'', '"'));
        }

        /* from Document */
        {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            assertNotNull(documentBuilderFactory);
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final XMLCell xmlCell = testSimpleConversion(Document.class, XMLCell.TYPE, XMLCell.class,
                builder.parse(new InputSource(new StringReader(xmlString))));
            assertEquals(xmlString, xmlCell.getStringValue().replace('\'', '"'));
        }

        /* from InputStream */
        try(final InputStream stream = getClass().getResourceAsStream("test.xml")) {
            assertTrue(stream.available() > 0);
            final XMLCell xmlCell = testSimpleConversion(InputStream.class, XMLCell.TYPE, XMLCell.class, stream);
            assertEquals(xmlString, xmlCell.getStringValue().replace('\'', '"'));
        }
    }

    /**
     * Test byte[] -> BinaryObjectDataCell and InputStream -> BinaryObjectDataCell conversions.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testToBinaryObjectDataCell() throws Exception {
        {
            final BinaryObjectDataCell cell = testSimpleConversion(byte[].class, BinaryObjectDataCell.TYPE,
                BinaryObjectDataCell.class, new String("I am bytes.").getBytes());
            final InputStream stream = cell.openInputStream();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                assertEquals("I am bytes.", reader.readLine());
            }
        }
        {
            final BinaryObjectDataCell cell = testSimpleConversion(InputStream.class, BinaryObjectDataCell.TYPE,
                BinaryObjectDataCell.class, new ByteArrayInputStream("I am bytes.".getBytes()));
            final InputStream stream = cell.openInputStream();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                assertEquals("I am bytes.", reader.readLine());
            }
        }
    }

    /**
     * Test Integer[] -> ListCell(IntCell) conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testCollectionTypes() throws Exception {
        final Integer[] coll = {0, 1, 4, 9, 16, 25, null};

        final Optional<JavaToDataCellConverterFactory<Integer[]>> factory = JavaToDataCellConverterRegistry
            .getInstance().getConverterFactories(Integer[].class, ListCell.getCollectionType(IntCell.TYPE)).stream().findFirst();
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<Integer[]> converter = factory.get().create(null);
        assertNotNull(converter);

        final DataCell cell = converter.convert(coll);
        assertTrue(cell instanceof ListCell);

        final ListCell listCell = (ListCell)converter.convert(coll);
        for (int i = 0; i < 6; ++i) {
            assertEquals(i * i, ((IntCell)listCell.get(i)).getIntValue());
        }
        assertTrue(listCell.get(6).isMissing());
    }

    /**
     * Test int[] -> ListCell(IntCell) conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testPrimitiveCollectionTypes() throws Exception {
        final int[] coll = {0, 1, 4, 9, 16, 25, 36}; // No missing values possible.

        final Optional<JavaToDataCellConverterFactory<int[]>> factory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(int[].class, ListCell.getCollectionType(IntCell.TYPE)).stream().findFirst();
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<int[]> converter = factory.get().create(null);
        assertNotNull(converter);

        final DataCell cell = converter.convert(coll);
        assertTrue(cell instanceof ListCell);

        final ListCell listCell = (ListCell)converter.convert(coll);
        for (int i = 0; i < 7; ++i) {
            assertEquals(i * i, ((IntCell)listCell.get(i)).getIntValue());
        }
    }

    /**
     * Test destination types of Integer/int and FileInputStream.
     */
    @Test
    public void testDestTypes() {
        final Set<DataType> destTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(Integer.class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertEquals(2, destTypes.size());
        assertTrue(destTypes.contains(IntCell.TYPE));
        assertTrue(destTypes.contains(LongCell.TYPE));

        // Primitive int type should map to the same cells as Integer.
        final Set<DataType> primitiveDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(int.class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        assertEquals(destTypes, primitiveDestTypes);

        final Collection<DataType> supertypeDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(FileInputStream.class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        assertThat("Not enough converters for conversion from FileInputStream",
            supertypeDestTypes.size(), is(greaterThanOrEqualTo(3)));
        assertTrue(supertypeDestTypes.contains(BinaryObjectDataCell.TYPE));
        assertTrue(supertypeDestTypes.contains(XMLCell.TYPE));
        assertTrue(supertypeDestTypes.contains(StringCell.TYPE));
    }

    /**
     * Test destination types of Integer/int and FileInputStream arrays.
     */
    @Test
    public void testCollectionDestTypes() {
        final Set<DataType> destTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(Integer[].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertEquals(2, destTypes.size());
        assertTrue(destTypes.contains(ListCell.getCollectionType(IntCell.TYPE)));
        assertTrue(destTypes.contains(ListCell.getCollectionType(LongCell.TYPE)));

        // Primitive int[] type should map to the same cells as Integer[]
        // (+ also maps to Byte vector which is irrelevant here).
        final Set<DataType> primitiveDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(int[].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        primitiveDestTypes.containsAll(destTypes);

        final Collection<DataType> supertypeDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(FileInputStream[].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        assertThat("Not enough converters for conversion from FileInputStream[]", supertypeDestTypes.size(),
            is(greaterThanOrEqualTo(3)));
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(BinaryObjectDataCell.TYPE)));
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(XMLCell.TYPE)));
        assertTrue(supertypeDestTypes.contains(ListCell.getCollectionType(StringCell.TYPE)));
    }

    /**
     * Test destination types of Integer/int arrays of arrays.
     */
    @Test
    public void testNestedCollectionDestTypes() {
        final Set<DataType> destTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(Integer[][].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());

        assertEquals(2, destTypes.size());
        assertTrue(destTypes.contains(ListCell.getCollectionType(ListCell.getCollectionType(IntCell.TYPE))));
        assertTrue(destTypes.contains(ListCell.getCollectionType(ListCell.getCollectionType(LongCell.TYPE))));

        // Primitive int[][] type should map to the same cells as Integer[][]
        // (+ also maps to Byte vector collection which is irrelevant here).
        final Set<DataType> primitiveDestTypes =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForSourceType(int[][].class).stream()
                .map((factory) -> factory.getDestinationType()).collect(Collectors.toSet());
        primitiveDestTypes.containsAll(destTypes);
    }

    /**
     * Test source types for DoubeCell.TYPE.
     */
    @Test
    public void testSourceTypes() {
        final Collection<JavaToDataCellConverterFactory<?>> factories =
            JavaToDataCellConverterRegistry.getInstance().getFactoriesForDestinationType(DoubleCell.TYPE);

        final Collection<Class<?>> set =
            factories.stream().map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertEquals(2, set.size());
        assertTrue(set.contains(Double.class));
        // Primitive type converter:
        assertTrue(set.contains(double.class));
    }

    /**
     * Test source types for ListCell(IntCell.TYPE).
     */
    @Test
    public void testArraySourceTypes() {
        final Collection<JavaToDataCellConverterFactory<?>> factories = JavaToDataCellConverterRegistry.getInstance()
            .getFactoriesForDestinationType(ListCell.getCollectionType(IntCell.TYPE));

        final Collection<Class<?>> set =
            factories.stream().map((factory) -> factory.getSourceType()).collect(Collectors.toSet());

        assertThat("Not enough converters for conversion to IntCell list ", factories.size(), is(greaterThanOrEqualTo(2)));
        assertTrue(set.contains(Integer[].class));
        // Primitive type converter:
        assertTrue(set.contains(int[].class));
        // disabled, see class header org.knime.core.data.convert.ExtensionPointTest
        // assertTrue(set.contains(String[].class));
    }

    /**
     * Test {@link JavaToDataCellConverterRegistry#getAllSourceTypes()}.
     */
    @Test
    public void testAllSourceTypes() {
        final Collection<Class<?>> set = JavaToDataCellConverterRegistry.getInstance().getAllSourceTypes();

        // extensions may increase this number, which is why we test greaterThan
        assertThat("Unexpected number of supported source types", set.size(), is(greaterThan(13)));
        assertThat("Double not found in supported source types", set, hasItem(Double.class));
    }

    /**
     * Test {@link JavaToDataCellConverterRegistry#getAllDestinationTypes()}.
     */
    @Test
    public void testAllDestinationTypes() {
        final Collection<DataType> set = JavaToDataCellConverterRegistry.getInstance().getAllDestinationTypes();

        // extensions may increase this number, which is why we test greaterThan
        assertThat("Unexpected number of supported destination types", set.size(), is(greaterThan(6)));
        assertThat("Double not found in supported destination types", set, hasItem(DoubleCell.TYPE));
    }
}
