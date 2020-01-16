package com.reedelk.xml.component;

import com.reedelk.runtime.api.commons.FileUtils;
import com.reedelk.runtime.api.commons.ModuleContext;
import com.reedelk.runtime.api.converter.ConverterService;
import com.reedelk.runtime.api.message.FlowContext;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.message.MessageAttributes;
import com.reedelk.runtime.api.message.MessageBuilder;
import com.reedelk.runtime.api.script.ScriptEngineService;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XPathComponentTest {

    private XPathComponent component;

    @Mock
    private FlowContext context;
    @Mock
    private ConverterService converterService;
    @Mock
    private ScriptEngineService scriptEngineService;

    @BeforeEach
    void setUp() {
        component = new XPathComponent();
        setUpMockConverterService();
        setUpScriptEngineService();
    }

    @Nested
    @DisplayName("Static XQuery Expression")
    class StaticXQueryExpression {

        @Test
        void shouldCorrectlyMatchXPathUsingNamespace() {
            // Given
            String xmlDocument = resourceAsString("/fixture/book_store.xml");

            Map<String, String> prefixNamespaces = new HashMap<>();
            prefixNamespaces.put("ns2", "http://bookstore.com/schemes");
            XPathConfiguration configuration = new XPathConfiguration();
            configuration.setPrefixNamespaceMap(prefixNamespaces);
            DynamicString xPathExpression =
                    DynamicString.from("//ns2:bookStore/ns2:book/ns2:name/text()");

            component.setExpression(xPathExpression);
            component.setConfiguration(configuration);
            component.initialize();

            Message message = MessageBuilder.get().withText(xmlDocument).build();

            // When
            Message result = component.apply(message, context);

            // Then
            List<String> xPathResult = result.payload();
            assertThat(xPathResult).containsExactlyInAnyOrder("Data Structure", "Java Core");
        }

        @Test
        void shouldGetBookTitles() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            DynamicString xPathExpression = DynamicString.from("//book[@year>2001]/title/text()");

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            // When
            Message result = component.apply(message, context);

            // Then
            List<String> xPathResult = result.payload();
            assertThat(xPathResult).containsExactlyInAnyOrder("Burning Tower");
        }

        @Test
        void shouldCountAllBookTitles() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            DynamicString xPathExpression = DynamicString.from("count(//book/title)");

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            // When
            Message result = component.apply(message, context);

            // Then
            BigInteger count = result.payload();
            assertThat(count).isEqualTo(3);
        }

        @Test
        void shouldReturnCorrectBooleanResult() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            // Exists book with price greater than 14.
            DynamicString xPathExpression = DynamicString.from("boolean(/inventory/book/price[text() > 14])");

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            // When
            Message result = component.apply(message, context);

            // Then
            boolean existsBookWithPriceGreaterThan14 = result.payload();
            assertThat(existsBookWithPriceGreaterThan14).isTrue();
        }

        @Test
        void shouldAddCorrectAttributesInOutputMessage() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            DynamicString xPathExpression = DynamicString.from("//book[@year>2001]/title/text()");

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            // When
            Message result = component.apply(message, context);

            // Then
            MessageAttributes attributes = result.getAttributes();
            assertThat(attributes).containsEntry("xPathExpression", "//book[@year>2001]/title/text()");
        }
    }

    @Nested
    @DisplayName("Dynamic XQuery Expression")
    class DynamicXQueryExpression {

        private ModuleContext moduleContext = new ModuleContext(10L);

        @Test
        void shouldCorrectlyEvaluateDynamicExpression() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");

            DynamicString xPathExpression =
                    DynamicString.from("#['//book[@year>2001]/title/text()']", moduleContext);

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            doReturn(Optional.of("//book[@year>2001]/title/text()"))
                    .when(scriptEngineService)
                    .evaluate(xPathExpression, context, message);

            // When
            Message result = component.apply(message, context);

            // Then
            List<String> xPathResult = result.payload();
            assertThat(xPathResult).containsExactly("Burning Tower");
        }

        @Test
        void shouldReturnEmptyMessageWhenEvaluatedExpressionIsEmpty() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            DynamicString xPathExpression =
                    DynamicString.from("#['//book[@year>2001]/title/text()']", moduleContext);

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            doReturn(Optional.empty())
                    .when(scriptEngineService)
                    .evaluate(xPathExpression, context, message);

            // When
            Message result = component.apply(message, context);

            // Then
            Object payload = result.payload();
            assertThat(payload).isNull();
        }

        @Test
        void shouldAddCorrectAttributesInOutputMessage() {
            // Given
            String xml = resourceAsString("/fixture/book_inventory.xml");
            DynamicString xPathExpression =
                    DynamicString.from("#['//book[@year>2001]/title/text()']", moduleContext);

            component.setExpression(xPathExpression);
            component.initialize();

            Message message = MessageBuilder.get().withText(xml).build();

            doReturn(Optional.of("//book[@year>2001]/title/text()"))
                    .when(scriptEngineService)
                    .evaluate(xPathExpression, context, message);

            // When
            Message result = component.apply(message, context);

            // Then
            MessageAttributes attributes = result.getAttributes();
            assertThat(attributes).containsEntry("xPathExpression", "//book[@year>2001]/title/text()");
        }
    }

    private String resourceAsString(String resourceFile) {
        URL url = XPathComponentTest.class.getResource(resourceFile);
        return FileUtils.ReadFromURL.asString(url);
    }

    private void setUpScriptEngineService() {
        setComponentFieldWithObject("scriptEngine", scriptEngineService);
    }

    private void setUpMockConverterService() {
        when(converterService.convert(any(Object.class), eq(byte[].class))).thenAnswer(invocation -> {
            String actualValue = invocation.getArgument(0);
            return actualValue.getBytes();
        });
        setComponentFieldWithObject("converterService", converterService);
    }

    private void setComponentFieldWithObject(String field, Object object) {
        try {
            Field converterServiceField = component.getClass().getDeclaredField(field);
            converterServiceField.setAccessible(true);
            converterServiceField.set(component, object);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            fail(e.getMessage(), e);
        }
    }
}