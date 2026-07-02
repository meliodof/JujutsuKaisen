package com.example.JujutsuKaisen.DistributionMachines.Exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidMileageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMileage(InvalidMileageException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({VehicleBusyException.class, DriverBusyException.class})
    public ResponseEntity<Map<String, Object>> handleBusy(RuntimeException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", e.getStatusCode().value());
        body.put("error", HttpStatus.valueOf(e.getStatusCode().value()).getReasonPhrase());
        body.put("message", e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 409);
        body.put("error", "Conflict");

        String message = e.getMostSpecificCause().getMessage();
        String userMessage = "Невозможно удалить: сущность используется в других записях";

        Pattern pattern = Pattern.compile("таблице \"(\\w+)\"");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            String tableDescription = getTableDescription(tableName);
            userMessage = "Невозможно удалить: запись используется в " + tableDescription;
        }

        body.put("message", userMessage);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e) {
        e.printStackTrace();  // ← добавить эту строку
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "Внутренняя ошибка сервера");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getTableDescription(String tableName) {
        switch (tableName) {
            case "trip_assignment": return "назначенных рейсах";
            case "route_stop_order": return "связях маршрутов и остановок";
            case "schedule_template": return "шаблонах расписания";
            case "vehicle": return "машинах";
            case "driver": return "водителях";
            case "route": return "маршрутах";
            case "stop": return "остановках";
            default: return "других записях (" + tableName + ")";
        }
    }

}