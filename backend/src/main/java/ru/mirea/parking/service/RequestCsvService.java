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
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.dto.request.RequestCreateRequest;
import ru.mirea.parking.dto.request.RequestResponse;
import ru.mirea.parking.dto.request.RequestUpdateRequest;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.repository.OwnerRepository;
import ru.mirea.parking.repository.RequestRepository;
import ru.mirea.parking.repository.RequestStatusRepository;
import ru.mirea.parking.repository.SpotRepository;
import ru.mirea.parking.repository.VehicleRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Экспорт и импорт заявок в CSV через CsvSupport; ключ — код заявки, без кода строка оформляет новую заявку. */
@Service
public class RequestCsvService {

    public static final String ENTITY = "requests";
    public static final String CODE = "код";
    public static final String OWNER_PHONE = "телефон_владельца";
    public static final String PLATE = "гос_номер";
    public static final String SPOT = "номер_места";
    public static final String START = "начало";
    public static final String END = "окончание";
    public static final String STATUS = "статус";
    public static final String DESCRIPTION = "описание";
    public static final String PAID = "оплачена";
    public static final String TAGS = "теги";

    /** Обязательные столбцы импорта. */
    public static final List<String> REQUIRED = List.of(OWNER_PHONE, PLATE, SPOT, START, END);
    /** Столбцы экспорта: те же, что читает импорт, плюс справочные (владелец, тип места, стоимость, приоритет, архив). */
    public static final List<String> EXPORT = List.of(CODE, OWNER_PHONE, PLATE, SPOT, START, END, STATUS, DESCRIPTION,
            PAID, TAGS, "владелец", "тип_места", "стоимость", "приоритет", "архив");

    private static final List<DateTimeFormatter> ACCEPTED_DATE_TIMES = List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

    private final CsvSupport csv;
    private final RequestService requestService;
    private final RequestRepository requests;
    private final OwnerRepository owners;
    private final VehicleRepository vehicles;
    private final SpotRepository spots;
    private final RequestStatusRepository statuses;
    private final Clock clock;

    public RequestCsvService(CsvSupport csv, RequestService requestService, RequestRepository requests,
                             OwnerRepository owners, VehicleRepository vehicles, SpotRepository spots,
                             RequestStatusRepository statuses, Clock clock) {
        this.csv = csv;
        this.requestService = requestService;
        this.requests = requests;
        this.owners = owners;
        this.vehicles = vehicles;
        this.spots = spots;
        this.statuses = statuses;
        this.clock = clock;
    }

    /** Разобранная строка файла; line нужен как ключ для строк без кода. */
    record Row(int line, String code, Owner owner, Vehicle vehicle, Spot spot, LocalDateTime start, LocalDateTime end,
               RequestStatus status, String description, boolean paid, Set<String> tags) {

        String key() {
            return code != null ? code : "строка " + line;
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(RequestFilter filter) {
        List<List<?>> rows = requestService.searchAll(filter).stream().map(RequestCsvService::toCsv).toList();
        return csv.attachment("requests-" + LocalDate.now(clock) + ".csv", csv.write(EXPORT, rows));
    }

    public ImportPreview preview(MultipartFile file) {
        return csv.preview(ENTITY, file, REQUIRED, this::parse, Row::key);
    }

    public ImportResult apply(ImportApplyRequest request) {
        return csv.apply(ENTITY, request, REQUIRED, this::parse, Row::key, this::write);
    }

    private static List<?> toCsv(RequestResponse r) {
        return Arrays.asList(r.code(), r.owner().phone(), r.vehicle().licensePlate(), r.spot().number(),
                r.startTime(), r.endTime(), r.status().code(), r.description(),
                r.paid(), String.join(",", r.tags()), r.owner().name(), r.spot().typeTitle(), r.cost(),
                r.priority().name(), r.archived());
    }

    /** Разбор строки: владелец по телефону, автомобиль по номеру, место по номеру; автомобиль должен быть этого владельца. */
    Row parse(CsvRow row) {
        String code = row.optional(CODE);
        if (code != null) {
            code = code.toUpperCase();
        }
        Owner owner = findOwner(row.required(OWNER_PHONE));
        Vehicle vehicle = findVehicle(row.required(PLATE));
        if (!vehicle.getOwner().getId().equals(owner.getId())) {
            throw new CsvRowException("Автомобиль " + vehicle.getLicensePlate() + " не принадлежит владельцу " + owner.getName());
        }
        Spot spot = findSpot(row.requiredInt(SPOT));
        LocalDateTime start = dateTime(row, START);
        LocalDateTime end = dateTime(row, END);
        if (!end.isAfter(start)) {
            throw new CsvRowException("Окончание должно быть позже начала");
        }
        RequestStatus status = row.optional(STATUS) == null ? null : findStatus(row.get(STATUS));
        String description = row.optional(DESCRIPTION);
        if (description != null && description.length() > 500) {
            throw new CsvRowException("Столбец «" + DESCRIPTION + "» длиннее 500 символов");
        }
        Set<String> tags = Arrays.stream(row.get(TAGS).split("[,;]"))
                .map(RequestService::normalizeTag).filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Row(row.line(), code, owner, vehicle, spot, start, end, status, description, row.bool(PAID), tags);
    }

    /** Без кода — новая заявка; с кодом CREATE_ONLY пропускает её, UPSERT обновляет. Проверки периода и пересечений — в RequestService. */
    private Outcome write(Row row, ImportMode mode) {
        if (row.code() == null) {
            RequestResponse created = requestService.create(new RequestCreateRequest(row.owner().getUuid(),
                    row.vehicle().getUuid(), row.spot().getUuid(), row.start(), row.end(), row.description(),
                    row.paid(), row.tags()));
            if (row.status() != null && !RequestStatus.NEW.equals(row.status().getCode())) {
                requestService.update(created.id(), new RequestUpdateRequest(created.vehicle().id(), created.spot().id(),
                        created.startTime(), created.endTime(), row.status().getCode(), created.description(),
                        created.paid(), created.tags(), created.version()));
            }
            return Outcome.CREATED;
        }
        Optional<Request> existing = requests.findByCode(row.code());
        if (existing.isEmpty()) {
            throw new CsvRowException("Заявка " + row.code() + " не найдена: код присваивается системой, для новой заявки оставьте его пустым");
        }
        if (mode == ImportMode.CREATE_ONLY) {
            return Outcome.SKIPPED;
        }
        Request request = existing.get();
        if (!request.getOwner().getId().equals(row.owner().getId())) {
            throw new CsvRowException("Владельца заявки " + row.code() + " изменить нельзя");
        }
        String status = row.status() == null ? request.getStatus().getCode() : row.status().getCode();
        requestService.update(request.getUuid(), new RequestUpdateRequest(row.vehicle().getUuid(), row.spot().getUuid(),
                row.start(), row.end(), status, row.description(), row.paid(), row.tags(), request.getVersion()));
        return Outcome.UPDATED;
    }

    private Owner findOwner(String rawPhone) {
        return owners.findByPhone(VehicleCsvService.normalizePhone(rawPhone))
                .orElseThrow(() -> new CsvRowException("Владелец с телефоном " + rawPhone + " не найден"));
    }

    private Vehicle findVehicle(String rawPlate) {
        String plate = LicensePlates.normalizeOrNull(rawPlate);
        if (plate == null) {
            throw new CsvRowException("Неверный гос. номер «" + rawPlate + "». " + LicensePlates.FORMAT_MESSAGE);
        }
        return vehicles.findByLicensePlate(plate)
                .orElseThrow(() -> new CsvRowException("Автомобиль " + plate + " не найден"));
    }

    private Spot findSpot(int number) {
        return spots.findBySpotNumber(number)
                .orElseThrow(() -> new CsvRowException("Место " + number + " не найдено"));
    }

    /** Статус по коду (NEW) или по названию («Новая»). */
    private RequestStatus findStatus(String value) {
        return statuses.findAll().stream()
                .filter(s -> s.getCode().equalsIgnoreCase(value) || s.getTitle().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new CsvRowException("Столбец «" + STATUS + "»: неизвестное значение «" + value + "»"));
    }

    /** Дата и время: 2026-11-25 09:00, 2026-11-25T09:00 или 25.11.2026 09:00. */
    private static LocalDateTime dateTime(CsvRow row, String header) {
        String value = row.required(header);
        for (DateTimeFormatter format : ACCEPTED_DATE_TIMES) {
            try {
                return LocalDateTime.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // пробуем следующий формат
            }
        }
        throw new CsvRowException("Столбец «" + header + "»: «" + value + "» не дата и время, ожидается 2026-11-25 09:00");
    }
}
