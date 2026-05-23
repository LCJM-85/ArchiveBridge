package edu.scau.scauarchiveinsight.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import java.io.IOException;

public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final DesensitizeUtil.SensitiveType type;

    public DesensitizeSerializer() {
        this.type = null;
    }

    public DesensitizeSerializer(DesensitizeUtil.SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (!DesensitizeContext.isEnabled() || value == null || type == null) {
            gen.writeString(value);
            return;
        }
        gen.writeString(DesensitizeUtil.mask(type, value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, com.fasterxml.jackson.databind.BeanProperty property) {
        if (property != null) {
            Sensitive ann = property.getAnnotation(Sensitive.class);
            if (ann != null) {
                return new DesensitizeSerializer(ann.value());
            }
        }
        return new StringSerializer();
    }
}
