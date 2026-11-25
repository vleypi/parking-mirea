package ru.mirea.parking.csv;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import ru.mirea.parking.error.BadRequestException;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Запись и чтение CSV: BOM, разделитель, защита от формул, кодировки, заголовки и лимиты. */
class CsvSupportTest {

    private final CsvSupport csv = new CsvSupport(new ImportTokenStore(Clock.systemUTC()), mock(PlatformTransactionManager.class));

    @Test
    void writesExcelFriendlyCsv() {
        byte[] bytes = csv.write(List.of("номер", "тариф", "крытое"),
                List.of(List.of(5, new BigDecimal("150.50"), true)));

        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        String text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertThat(text).isEqualTo("номер;тариф;крытое\r\n5;150,50;да\r\n");
    }

    @Test
    void neutralizesFormulasAndRestoresThemOnRead() {
        byte[] bytes = csv.write(List.of("телефон", "описание"),
                List.of(List.of("+7-900-100-00-01", "=HYPERLINK(\"http://evil\")")));
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertThat(text).contains("\n'+7-900-100-00-01;").doesNotContain(";=HYPERLINK").contains("\"'=HYPERLINK");

        List<CsvRow> rows = csv.read(bytes, List.of("телефон"));
        assertThat(rows.getFirst().get("телефон")).isEqualTo("+7-900-100-00-01");
    }

    @Test
    void readsHeadersCaseInsensitiveWithRowNumbers() {
        String file = "Номер;Тариф\n1;100\n\n2;120,5\n";
        List<CsvRow> rows = csv.read(file.getBytes(StandardCharsets.UTF_8), List.of("номер", "тариф"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).line()).isEqualTo(2);
        assertThat(rows.get(1).requiredDecimal("тариф")).isEqualByComparingTo("120.5");
    }

    @Test
    void acceptsWindows1251AndCommaDelimiter() {
        byte[] bytes = "номер,тип\n7,СТАНДАРТ\n".getBytes(Charset.forName("windows-1251"));
        List<CsvRow> rows = csv.read(bytes, List.of("номер", "тип"));
        assertThat(rows.getFirst().get("тип")).isEqualTo("СТАНДАРТ");
    }

    @Test
    void missingHeadersAndEmptyFileAreBadRequest() {
        assertThatThrownBy(() -> csv.read("номер\n1\n".getBytes(StandardCharsets.UTF_8), List.of("номер", "тариф")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("тариф");
        assertThatThrownBy(() -> csv.read(new byte[0], List.of("номер")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void tooManyRowsIsBadRequest() {
        StringBuilder file = new StringBuilder("номер\n");
        for (int i = 0; i <= CsvSupport.MAX_ROWS; i++) {
            file.append(i).append('\n');
        }
        assertThatThrownBy(() -> csv.read(file.toString().getBytes(StandardCharsets.UTF_8), List.of("номер")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(String.valueOf(CsvSupport.MAX_ROWS));
    }

    @Test
    void rowParsingHelpersGiveReadableErrors() {
        CsvRow row = csv.read("номер;крытое\nабв;может быть\n".getBytes(StandardCharsets.UTF_8),
                List.of("номер")).getFirst();
        assertThatThrownBy(() -> row.requiredInt("номер")).isInstanceOf(CsvRowException.class).hasMessageContaining("абв");
        assertThatThrownBy(() -> row.bool("крытое")).isInstanceOf(CsvRowException.class);
        assertThatThrownBy(() -> row.required("нет такого")).isInstanceOf(CsvRowException.class);
    }
}
