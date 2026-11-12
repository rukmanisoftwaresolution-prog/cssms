package com.cssms.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /** Trims every incoming string and converts blank strings to null so optional fields validate cleanly. */
    @Bean
    public SimpleModule blankStringToNullModule() {
        SimpleModule module = new SimpleModule("blank-string-to-null");
        module.addDeserializer(String.class, new StdScalarDeserializer<>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getValueAsString();
                if (text == null) {
                    return null;
                }
                String trimmed = text.trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
        });
        return module;
    }
}
