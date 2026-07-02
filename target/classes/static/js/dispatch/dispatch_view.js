var datePicker = document.getElementById('dispatchDate');
datePicker.value = new Date().toISOString().split('T')[0];

window.onload = loadAll;
datePicker.addEventListener('change', loadAll);

function loadAll() {
    var date = datePicker.value;
    loadMedical(date);
    loadInspections(date);
    loadFuel(date);
    loadVehicleControl(date);
    loadWaybills(date);
}

function loadVehicleControl(date) {
    var table = document.getElementById('controlTable');
    if (!table) return;
    fetch('/api/dispatcher/vehicle-control?date=' + date)
        .then(r => r.json())
        .then(data => {
            var withAlerts = data.filter(r => r.alert);
            table.innerHTML = withAlerts.length === 0
                ? '<tr><td colspan="6">Замечаний нет</td></tr>'
                : withAlerts.map(r => '<tr><td>' + r.reg_number + '</td><td>#' + r.trip_id + '</td><td>' + (r.driver_name || '—') + '</td><td>'
                    + (r.mileage_before || '—') + ' → ' + (r.mileage_after || '—') + '</td><td>'
                    + (r.fuel_before || '—') + ' / ' + (r.fuel_after || '—') + ' л</td><td><strong>' + r.alert + '</strong></td></tr>').join('');
        });
}

function loadMedical(date) {
    fetch('/api/admin/medical?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('medTable').innerHTML = data.length === 0
                ? '<tr><td colspan="5">Нет данных</td></tr>'
                : data.map(m => '<tr><td>' + m.driver_name + '</td><td>' + m.check_type + '</td><td>' + m.result + '</td><td>' + (m.notes || '') + '</td><td>' + new Date(m.check_date).toLocaleString() + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('medTable').innerHTML = '<tr><td colspan="5">Ошибка загрузки</td></tr>');
}

function loadInspections(date) {
    fetch('/api/admin/inspections?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('inspTable').innerHTML = data.length === 0
                ? '<tr><td colspan="6">Нет данных</td></tr>'
                : data.map(i => '<tr><td>' + (i.vehicle_reg || i.vehicle_id) + '</td><td>' + (i.driver_name || '') + '</td><td>' + i.inspection_type + '</td><td>' + i.result + '</td><td>' + (i.notes || '') + '</td><td>' + new Date(i.inspection_date).toLocaleString() + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('inspTable').innerHTML = '<tr><td colspan="6">Ошибка загрузки</td></tr>');
}

function loadFuel(date) {
    fetch('/api/admin/fuel?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('fuelTable').innerHTML = data.length === 0
                ? '<tr><td colspan="5">Нет данных</td></tr>'
                : data.map(f => '<tr><td>' + (f.vehicle_reg || f.vehicle_id) + '</td><td>' + (f.driver_name || '') + '</td><td>' + f.liters + '</td><td>' + (f.mileage || '') + '</td><td>' + new Date(f.fuel_date).toLocaleString() + '</td></tr>').join('');
        })
        .catch(() => document.getElementById('fuelTable').innerHTML = '<tr><td colspan="5">Ошибка загрузки</td></tr>');
}

function waybillBadge(ok) {
    return ok ? '<span class="badge bg-success">Да</span>' : '<span class="badge bg-secondary">Нет</span>';
}

function loadWaybills(date) {
    fetch('/api/dispatcher/waybills?date=' + date)
        .then(r => r.json())
        .then(data => {
            document.getElementById('waybillTable').innerHTML = data.length === 0
                ? '<tr><td colspan="6">Нет путевых листов</td></tr>'
                : data.map(w => {
                    var ready = w.ready ? '<span class="badge bg-success">Готов</span>' : '<span class="badge bg-warning text-dark">Не готов</span>';
                    var mileage = (w.mileage_before || '—') + ' → ' + (w.mileage_after || '—');
                    return '<tr><td>#' + w.trip_id + '</td><td>' + (w.route_number || '') + '</td><td>' + w.driver_name + '</td><td>' + w.vehicle_reg + '</td>'
                        + '<td>' + mileage + ' | Мед:' + waybillBadge(w.medical_ok) + ' Мех:' + waybillBadge(w.inspection_ok) + '</td><td>' + ready + '</td></tr>';
                }).join('');
        })
        .catch(() => document.getElementById('waybillTable').innerHTML = '<tr><td colspan="6">Ошибка загрузки</td></tr>');
}