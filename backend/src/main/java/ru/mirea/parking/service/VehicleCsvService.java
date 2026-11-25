package ru.mirea.parking.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.parking.csv.CsvRow;
import ru.mirea.parking.csv.CsvRowException;
import ru.mirea.parking.csv.CsvSupport;
import ru.mirea.parking.csv.CsvSupport.Outcome;
import ru.mirea.parking.csv.ImportMode;
import ru.mirea.parking.domain.LicensePlates;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.dto.vehicle.VehicleCreateRequest;
import ru.mirea.parking.dto.vehicle.VehicleResponse;
import ru.mirea.parking.dto.vehicle.VehicleUpdateRequest;
import ru.mirea.parking.filter.VehicleFilter;
import ru.mirea.parking.repository.OwnerRepository;
import ru.mirea.parking.repository.VehicleRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Экспорт и импорт автомобилей в CSV через CsvSupport; ключ записи — гос. номер, владелец задаётся телефоном. */
@Service
public class VehicleCsvService {

    public static final String ENTITY = "vehicles";
    public static final String PLATE = "гос_номер";
    public static final String BRAND = "марка";
    public static final String MODEL = "модель";
    public static final String COLOR = "цвет";
    public static final String YEAR = "год";
    public static final String ELECTRIC = "электромобиль";
    public static final String STATUS = "статус";
    public static final String OWNER_PHONE = "телефон_владельца";
    public static final String TAGS = "теги";

    /** Обязательные столбцы импорта. */
    public static final List<String> REQUIRED = List.of(PLATE, BRAND, MODEL, OWNER_PHONE);
    /** Столбцы экспорта: те же, что читает импорт, плюс справочные (владелец, активность, архив). */
    public static final List<String> EXPORT = List.of(PLATE, BRAND, MODEL, COLOR, YEAR, ELECTRIC, STATUS, OWNER_PHONE,
            "владелец", "активность", TAGS, "архив");

    private final CsvSupport csv;
    private final VehicleService vehicleService;
    private final VehicleRepository vehicles;
    private final OwnerRepository owners;
    private final Clock clock;

    public VehicleCsvService(CsvSupport csv, VehicleService vehicleService, VehicleRepository vehicles,
                             OwnerRepository owners, Clock clock) {
        this.csv = csv;
        this.vehicleService = vehicleService;
        this.vehicles = vehicles;
        this.owners = owners;
        this.clock = clock;
    }

    /** Разобранная строка файла. */
    record Row(String plate, String brand, String model, String color, Short year, boolean electric,
               VehicleStatus status, Owner owner, Set<String> tags) {
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(VehicleFilter filter) {
        List<List<?>> rows = vehicleService.searchAll(filter).stream().map(VehicleCsvService::toCsv).toList();
        return csv.attachment("vehicles-" + LocalDate.now(clock) + ".csv", csv.write(EXPORT, rows));
    }

    public ImportPreview preview(MultipartFile file) {
        return csv.preview(ENTITY, file, REQUIRED, this::parse, Row::plate);
    }

    public ImportResult apply(ImportApplyRequest request) {
        return csv.apply(ENTITY, request, REQUIRED, this::parse, Row::plate, this::write);
    }

    private static List<?> toCsv(VehicleResponse v) {
        return Arrays.asList(v.licensePlate(), v.brand(), v.model(), v.color(), v.releaseYear(), v.electric(),
                v.status().code(), v.owner().phone(), v.owner().name(), v.activity().title(),
                String.join(",", v.tags()), v.archived());
    }

    /** Разбор строки: те же проверки, что у формы (формат номера, длины, год, владелец не в архиве). */
    Row parse(CsvRow row) {
        String plate = LicensePlates.normalizeOrNull(row.required(PLATE));
        if (plate == null) {
            throw new CsvRowException("Неверный гос. номер «" + row.get(PLATE) + "». " + LicensePlates.FORMAT_MESSAGE);
        }
        String brand = limited(row.required(BRAND), BRAND, 50);
        String model = limited(row.required(MODEL), MODEL, 50);
        String color = row.optional(COLOR) == null ? null : limited(row.get(COLOR), COLOR, 30);
        Integer year = row.optionalInt(YEAR);
        int currentYear = Year.now(clock).getValue();
        if (year != null && (year < 1980 || year > currentYear)) {
            throw new CsvRowException("Год выпуска от 1980 до " + currentYear + ", получено " + year);
        }
        VehicleStatus status = row.optional(STATUS) == null ? VehicleStatus.ACTIVE
                : row.enumValue(STATUS, VehicleStatus.class, VehicleStatus::getTitle);
        Owner owner = findOwner(row.required(OWNER_PHONE));
        Set<String> tags = Arrays.stream(row.get(TAGS).split("[,;]"))
                .map(VehicleService::normalizeTag).filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Row(plate, brand, model, color, year == null ? null : year.shortValue(), row.bool(ELECTRIC),
                status, owner, tags);
    }

    /** CREATE_ONLY пропускает существующие номера, UPSERT обновляет их. */
    private Outcome write(Row row, ImportMode mode) {
        Optional<Vehicle> existing = vehicles.findByLicensePlate(row.plate());
        if (existing.isEmpty()) {
            vehicleService.create(new VehicleCreateRequest(row.owner().getUuid(), row.plate(), row.brand(), row.model(),
                    row.color(), row.year(), row.electric(), row.status(), row.tags()));
            return Outcome.CREATED;
        }
        if (mode == ImportMode.CREATE_ONLY) {
            return Outcome.SKIPPED;
        }
        Vehicle vehicle = existing.get();
        vehicleService.update(vehicle.getUuid(), new VehicleUpdateRequest(row.owner().getUuid(), row.plate(), row.brand(),
                row.model(), row.color(), row.year(), row.electric(), row.status(), row.tags(), vehicle.getVersion()));
        return Outcome.UPDATED;
    }

    private Owner findOwner(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        Owner owner = owners.findByPhone(phone)
                .orElseThrow(() -> new CsvRowException("Владелец с телефоном " + rawPhone + " не найден"));
        if (owner.isArchived()) {
            throw new CsvRowException("Владелец " + owner.getName() + " в архиве: оформить на него автомобиль нельзя");
        }
        return owner;
    }

    /** Телефон к виду +7-XXX-XXX-XX-XX: принимаются 8XXXXXXXXXX, +7XXXXXXXXXX и уже отформатированный номер. */
    static String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.charAt(0) == '7' || digits.charAt(0) == '8')) {
            digits = digits.substring(1);
        }
        if (digits.length() != 10) {
            return raw.trim();
        }
        return "+7-%s-%s-%s-%s".formatted(digits.substring(0, 3), digits.substring(3, 6), digits.substring(6, 8),
                digits.substring(8));
    }

    private static String limited(String value, String header, int max) {
        if (value.length() > max) {
            throw new CsvRowException("Столбец «" + header + "» длиннее " + max + " символов");
        }
        return value;
    }
}
