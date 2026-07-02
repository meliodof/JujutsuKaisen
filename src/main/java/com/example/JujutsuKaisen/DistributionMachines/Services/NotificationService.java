package com.example.JujutsuKaisen.DistributionMachines.Services;

import com.example.JujutsuKaisen.DistributionMachines.EntityType;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepNotification;
import com.example.JujutsuKaisen.DistributionMachines.Repositories.RepSchedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {

    public static final String TYPE_TRIP_CANCEL = "TRIP_CANCEL";
    public static final String TYPE_TRIP_ISSUE = "TRIP_ISSUE";
    public static final String TYPE_MAINTENANCE = "MAINTENANCE_DUE";

    private final RepNotification repository;
    private final RepSchedule scheduleRepository;
    private final LogService logService;
    private final SseNotificationHub sseHub;

    public NotificationService(RepNotification repository, RepSchedule scheduleRepository,
                               LogService logService, SseNotificationHub sseHub) {
        this.repository = repository;
        this.scheduleRepository = scheduleRepository;
        this.logService = logService;
        this.sseHub = sseHub;
    }

    @Transactional
    public Map<String, Object> notifyDispatcher(String type, String title, String message,
                                                Integer tripId, Integer vehicleId, Integer driverId) {
        Integer id = repository.save(type, title, message, tripId, vehicleId, driverId);
        logService.log("NOTIFY", String.valueOf(EntityType.TRIP), tripId, "system", type + ": " + message);

        Map<String, Object> payload = repository.findById(id).orElse(new LinkedHashMap<>());
        sseHub.broadcast(payload);
        return payload;
    }

    @Transactional
    public void notifyTripCancelled(Integer tripId, String details, String reportedBy) {
        Map<String, Object> ctx = scheduleRepository.getTripContext(tripId);
        String reason = "vehicle_broken".equals(details) ? "поломка машины"
                : "driver_sick".equals(details) ? "водитель заболел" : details;
        String driverName = String.valueOf(ctx.getOrDefault("driver_name", "—"));
        String routeNum = String.valueOf(ctx.getOrDefault("route_number", ""));
        String routeName = String.valueOf(ctx.getOrDefault("route_name", ""));
        String route = (routeNum + " " + routeName).trim();
        if (route.isEmpty()) route = "—";
        String title = "Рейс отменён водителем";
        String message = String.format("Рейс #%s, маршрут %s, водитель %s (%s). Причина: %s",
                tripId, route, driverName, reportedBy != null ? reportedBy : "водитель", reason);

        notifyDispatcher(TYPE_TRIP_CANCEL, title, message, tripId,
                (Integer) ctx.get("vehicle_id"), (Integer) ctx.get("driver_id"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotifications(int limit, boolean unreadOnly) {
        return repository.findRecent(limit, unreadOnly);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSinceId(Integer sinceId, int limit) {
        return repository.findSinceId(sinceId, limit);
    }

    @Transactional(readOnly = true)
    public int countUnread() {
        return repository.countUnread();
    }

    @Transactional
    public void markRead(Integer id) {
        repository.markRead(id);
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead();
    }

    public SseEmitter subscribeStream() {
        return sseHub.subscribe();
    }
}
