var datePicker = document.getElementById('dispatchDate');
datePicker.value = new Date().toISOString().split('T')[0];
datePicker.addEventListener('change', loadAll);
window.onload = loadAll;

function loadAll() {
    var date = datePicker.value;
    loadMedical(date);
    loadInspections(date);
    loadFuel(date);
    loadVehicleControl(date);
    loadWaybills(date);
}

function loadVehicleControl(date) {
    fetch('/api/dispatcher/vehicle-control?date=' + date)
        .then(r => r.json())
        .then(data => {
            var withAlerts = data.filter(r => r.alert);
            document.getElementById('controlTable').innerHTML = withAlerts.length === 0
                ? '<tr><td colspan="6">Замечаний нет</td></tr>'
                : withAlerts.map(r => {
                    var mileage = (r.mileage_before || r.start_mileage || '—') + ' → ' + (r.mileage_after || r.end_mileage || '—');
                    var fuel = (r.fuel_before || '—') + ' / ' + (r.fuel_after || '—') + ' л';
                    return '<tr><td>' + r.reg_number + '</td><td>#' + r.trip_id + '</td><td>' + (r.driver_name || '—') + '</td>'
                        + '<td>' + mileage + '</td><td>' + fuel + '</td><td><strong>' + r.alert + '</strong></td></tr>';
                }).join('');
        })
        .catch(() => document.getElementById('controlTable').innerHTML = '<tr><td colspan="6">Ошибка загрузки</td></tr>');
}

function loadMedical(date) {
    fetch('/api/dispatcher/medical?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('medTable').innerHTML = data.length === 0
                ? '<tr><td colspan="5">Нет данных</td></tr>'
                : data.map(m => '<tr><td>' + m.driver_name + '</td><td>' + m.check_type + '</td><td>' + m.result + '</td><td>' + (m.notes || '') + '</td><td>' + (m.check_time || m.check_date) + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('medTable').innerHTML = '<tr><td colspan="5">Ошибка загрузки</td></tr>');
}

function loadInspections(date) {
    fetch('/api/dispatcher/inspections?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('inspTable').innerHTML = data.length === 0
                ? '<tr><td colspan="5">Нет данных</td></tr>'
                : data.map(i => '<tr><td>' + (i.vehicle_reg || i.vehicle_id) + '</td><td>' + (i.driver_name || '—') + '</td><td>' + i.inspection_type + '</td><td>' + i.result + '</td><td>' + (i.notes || '') + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('inspTable').innerHTML = '<tr><td colspan="5">Ошибка загрузки</td></tr>');
}

function loadFuel(date) {
    fetch('/api/dispatcher/fuel?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('fuelTable').innerHTML = data.length === 0
                ? '<tr><td colspan="4">Нет данных</td></tr>'
                : data.map(f => '<tr><td>' + (f.vehicle_reg || f.vehicle_id) + '</td><td>' + (f.driver_name || '—') + '</td><td>' + f.liters + '</td><td>' + (f.mileage || '—') + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('fuelTable').innerHTML = '<tr><td colspan="4">Ошибка загрузки</td></tr>');
}

function waybillBadge(ok) {
    return ok ? '<span class="badge bg-success">Да</span>' : '<span class="badge bg-secondary">Нет</span>';
}

function loadWaybills(date) {
    fetch('/api/dispatcher/waybills?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('waybillTable').innerHTML = data.length === 0
                ? '<tr><td colspan="9">Нет путевых листов — создайте из раздела «Рейсы»</td></tr>'
                : data.map(w => {
                    var mileage = (w.mileage_before || '—') + ' → ' + (w.mileage_after || '—');
                    var fuel = (w.fuel_before || '—') + ' / ' + (w.fuel_after || '—') + ' л';
                    var dist = (w.mileage_before && w.mileage_after) ? (w.mileage_after - w.mileage_before) + ' км' : '—';
                    var ready = w.ready
                        ? '<span class="badge bg-success">Готов</span>'
                        : '<span class="badge bg-warning text-dark">Не готов</span>';
                    return '<tr><td>#' + w.trip_id + '</td><td>' + (w.route_number || '—') + '</td><td>' + w.driver_name + '</td><td>' + w.vehicle_reg + '</td>'
                        + '<td>' + waybillBadge(w.medical_ok) + '</td><td>' + waybillBadge(w.inspection_ok) + '</td>'
                        + '<td>' + mileage + ' (' + dist + ')</td><td>' + fuel + '</td><td>' + ready + '</td></tr>';
                }).join('');
        })
        .catch(() => document.getElementById('waybillTable').innerHTML = '<tr><td colspan="9">Ошибка загрузки</td></tr>');
}
