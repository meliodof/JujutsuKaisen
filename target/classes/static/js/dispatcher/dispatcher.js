var datePicker = document.getElementById('datePicker');
datePicker.value = new Date().toISOString().split('T')[0];
var reassignModal, waybillModal;

window.onload = function() {
    reassignModal = new bootstrap.Modal(document.getElementById('reassignModal'));
    waybillModal = new bootstrap.Modal(document.getElementById('waybillModal'));
    loadRoutes();
    loadTrips();
};

function loadRoutes() {
    fetch('/api/routes')
        .then(r => r.json())
        .then(routes => {
            document.getElementById('routeSelect').innerHTML = '<option value="">Выберите маршрут</option>' +
                routes.map(r => '<option value="' + r.id + '">' + r.number + ' - ' + r.name + '</option>').join('');
        });
}

function loadTemplates() {
    var routeId = document.getElementById('routeSelect').value;
    var select = document.getElementById('templateSelect');
    if (!routeId) { select.innerHTML = '<option value="">Сначала выберите маршрут</option>'; return; }
    fetch('/api/schedule/route/' + routeId)
        .then(r => r.json())
        .then(t => { select.innerHTML = t.map(x => '<option value="' + x.id + '">' + x.departureTime + '</option>').join(''); });
}

function loadAvailable() {
    var date = datePicker.value;
    Promise.all([
        fetch('/api/dispatcher/vehicles/available?date=' + date).then(r => r.json()),
        fetch('/api/dispatcher/drivers/available?date=' + date).then(r => r.json())
    ]).then(results => {
        document.getElementById('vehicleSelect').innerHTML = results[0].map(v => '<option value="' + v.id + '">' + (v.reg_number || v.regNumber) + '</option>').join('');
        document.getElementById('driverSelect').innerHTML = results[1].map(d => '<option value="' + d.id + '">' + (d.full_name || d.fullName) + '</option>').join('');
    });
}

function assignTrip() {
    var vehicleId = parseInt(document.getElementById('vehicleSelect').value);
    var driverId = parseInt(document.getElementById('driverSelect').value);
    var date = datePicker.value;

    fetch('/api/dispatcher/check-readiness?vehicleId=' + vehicleId + '&driverId=' + driverId + '&date=' + date)
        .then(r => r.json())
        .then(check => {
            var warnings = [];
            if (!check.med_ok) warnings.push('Медосмотр не пройден');
            if (!check.insp_ok) warnings.push('Машина не осмотрена');
            if (!check.waybill_ok) warnings.push('Нет путевого листа');
            if (!check.mileage_ok) warnings.push('Не указан пробег до выезда (механик/водитель)');
            if (!check.fuel_ok) warnings.push('Не указано топливо до выезда (механик)');
            if (warnings.length > 0 && !confirm(warnings.join('\n') + '\n\nВсё равно назначить рейс?')) return;

            var body = {
                routeId: parseInt(document.getElementById('routeSelect').value),
                vehicleId: vehicleId, driverId: driverId,
                scheduleTemplateId: parseInt(document.getElementById('templateSelect').value),
                assignmentDate: date, status: 'scheduled'
            };
            fetch('/api/dispatcher/trips', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
            }).then(res => {
                if (res.ok) { loadTrips(); showToast('Рейс назначен'); }
                else res.json().then(err => showToast(err.message || 'Ошибка', true));
            });
        });
}

function loadTrips() {
    fetch('/api/dispatcher/trips?date=' + datePicker.value)
        .then(r => r.json())
        .then(trips => {
            document.getElementById('tripTable').innerHTML = trips.length === 0
                ? '<tr><td colspan="12" class="empty-state">Нет рейсов</td></tr>'
                : trips.map(t => {
                    var actions = '';
                    if (t.status === 'scheduled' || t.status === 'in_progress') {
                        actions += '<button class="btn btn-sm btn-warning" onclick="openReassign(' + t.id + ')">Изменить</button> ';
                        actions += '<button class="btn btn-sm btn-danger" onclick="cancelTrip(' + t.id + ')">Отменить</button> ';
                        actions += '<button class="btn btn-sm btn-info" onclick="createWaybill(' + t.id + ')">Путевой лист</button> ';
                        actions += '<button class="btn btn-sm btn-outline-secondary" onclick="showWaybill(' + t.id + ')">📋</button>';
                    } else if (t.status === 'cancelled') {
                        actions += '<button class="btn btn-sm btn-success" onclick="restoreTrip(' + t.id + ')">Восстановить</button>';
                    } else { actions += '—'; }
                    var reason = t.details === 'vehicle_broken' ? 'Поломка' : t.details === 'driver_sick' ? 'Заболел' : '—';
                    var wb = '—';
                    if (t.waybill_id) {
                        wb = t.waybill_ready ? '<span class="badge bg-success">Готов</span>'
                            : '<span class="badge bg-warning text-dark">Мед:' + (t.medical_ok ? '✓' : '✗') + ' Мех:' + (t.inspection_ok ? '✓' : '✗') + '</span>';
                    }
                    return '<tr><td>' + t.id + '</td><td>' + (t.route_number || '—') + '</td><td>' + (t.departure_time || '—') + '</td><td>' + (t.planned_arrival || '—') + '</td><td>' + (t.actual_departure ? new Date(t.actual_departure).toLocaleTimeString() : '—') + '</td><td>' + (t.actual_arrival ? new Date(t.actual_arrival).toLocaleTimeString() : '—') + '</td><td>' + (t.vehicle_reg || '—') + '</td><td>' + (t.driver_name || '—') + '</td><td>' + t.status + '</td><td>' + wb + '</td><td>' + reason + '</td><td>' + actions + '</td></tr>';
                }).join('');
        });
}

function createWaybill(id) {
    fetch('/api/dispatcher/trips/' + id + '/waybill', { method: 'POST' })
        .then(res => {
            if (res.ok) showToast('Путевой лист создан');
            else res.json().then(err => showToast(err.message || 'Ошибка', true));
        });
}

function showWaybill(tripId) {
    fetch('/api/dispatcher/waybill/' + tripId)
        .then(r => r.json())
        .then(w => {
            document.getElementById('waybillContent').innerHTML =
                '<p><strong>Рейс #' + w.trip_id + '</strong></p>' +
                '<p>Водитель: ' + (w.driver_name || '—') + '</p>' +
                '<p>Машина: ' + (w.vehicle_reg || '—') + '</p><hr>' +
                '<p>Медосмотр: ' + (w.medical_ok ? 'Допущен' : 'Не допущен') + '</p>' +
                '<p>Осмотр машины: ' + (w.inspection_ok ? 'Исправна' : 'Неисправна') + '</p><hr>' +
                '<p>Топливо до: ' + (w.fuel_before || '—') + ' л | после: ' + (w.fuel_after || '—') + ' л</p>' +
                '<p>🛣 Пробег до: ' + (w.mileage_before || '—') + ' км | после: ' + (w.mileage_after || '—') + ' км</p><hr>' +
                '<p><strong>Статус: ' + (w.ready ? 'Готов к выезду' : 'Не готов') + '</strong></p>';
            waybillModal.show();
        });
}
function openReassign(tripId) {
    document.getElementById('reassignTripId').value = tripId;
    var date = datePicker.value;
    Promise.all([
        fetch('/api/dispatcher/vehicles/available?date=' + date).then(r => r.json()),
        fetch('/api/dispatcher/drivers/available?date=' + date).then(r => r.json())
    ]).then(results => {
        document.getElementById('reassignVehicle').innerHTML = results[0].map(v => '<option value="' + v.id + '">' + (v.reg_number || v.regNumber) + '</option>').join('');
        document.getElementById('reassignDriver').innerHTML = results[1].map(d => '<option value="' + d.id + '">' + (d.full_name || d.fullName) + '</option>').join('');
        reassignModal.show();
    });
}

function saveReassign() {
    var tripId = document.getElementById('reassignTripId').value;
    fetch('/api/dispatcher/trips/' + tripId + '/reassign', {
        method: 'PUT', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            vehicleId: parseInt(document.getElementById('reassignVehicle').value) || null,
            driverId: parseInt(document.getElementById('reassignDriver').value) || null
        })
    }).then(res => {
        if (res.ok) { reassignModal.hide(); loadTrips(); showToast('Назначение обновлено'); }
        else res.json().then(err => showToast(err.message || 'Ошибка', true));
    });
}

function cancelTrip(id) {
    if (!confirm('Отменить рейс?')) return;
    fetch('/api/trips/' + id + '/status', {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'cancelled' })
    }).then(res => {
        if (res.ok) { loadTrips(); showToast('Рейс отменён'); }
        else res.json().then(err => showToast(err.message || 'Ошибка', true));
    });
}

function restoreTrip(id) {
    if (!confirm('Восстановить рейс?')) return;
    fetch('/api/trips/' + id + '/status', {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'scheduled' })
    }).then(res => {
        if (res.ok) { loadTrips(); showToast('Рейс восстановлен'); }
        else res.json().then(err => showToast(err.message || 'Ошибка', true));
    });
}

function showToast(msg, err) {
    var d = document.createElement('div');
    d.style.cssText = 'position:fixed;top:20px;right:20px;padding:14px 20px;border-radius:6px;color:#fff;font-size:13px;z-index:9999;background:' + (err ? '#ef4444' : '#16a34a');
    d.textContent = msg;
    document.body.appendChild(d);
    setTimeout(() => d.remove(), 2500);
}