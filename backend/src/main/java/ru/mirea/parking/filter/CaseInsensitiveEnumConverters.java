package ru.mirea.parking.filter;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.RequestPriority;

import java.util.Locale;

@Configuration
public class CaseInsensitiveEnumConverters implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, CompareOp.class, s -> CompareOp.valueOf(upper(s)));
        registry.addConverter(String.class, ArchiveScope.class, s -> ArchiveScope.valueOf(upper(s)));
        registry.addConverter(String.class, RequestPriority.class, s -> RequestPriority.valueOf(upper(s)));
        registry.addConverter(String.class, AuditEventType.class, s -> AuditEventType.valueOf(upper(s)));
        registry.addConverter(String.class, AuditEntityType.class, s -> AuditEntityType.valueOf(upper(s)));
    }

    private static String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
