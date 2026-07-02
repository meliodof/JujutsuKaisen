function tripRequestHeaders() {
    return {
        'Content-Type': 'application/json',
        'X-Username': localStorage.getItem('username') || 'водитель'
    };
}

window.onload = loadSchedule;

function loadSchedule() {
    var driverId = localStorage.getItem('driverId');
    if (!driverId || driverId === 'null') driverId = 1;

    fetch('/api/driver/schedule/today?driverId=' + driverId)
        .then(r => r.json())
        .then(trips => {
            document.getElementById('todayCount').textContent = trips.length;
            document.getElementById('completedCount').textContent = trips.filter(t => t.status === 'completed').length;
            document.getElementById('inProgressCount').textContent = trips.filter(t => t.status === 'in_progress').length;

            document.getElementById('scheduleTable').innerHTML = trips.length === 0
                ? '<tr><td colspan="6" class="empty-state">Нет рейсов на сегодня</td></tr>'
                : trips.map((t, i) => {
                    var actions = '';
                    if (t.status === 'scheduled') {
                        actions += '<button class="btn btn-success" onclick="updateStatus(' + t.id + ', \'in_progress\')">Начать</button> ';
                        actions += '<button class="btn btn-danger" onclick="reportIssue(' + t.id + ', \'vehicle_broken\')">Поломка</button> ';
                        actions += '<button class="btn btn-warning" onclick="reportIssue(' + t.id + ', \'driver_sick\')">Заболел</button>';
                    } else if (t.status === 'in_progress') {
                        actions += '<button class="btn btn-info" onclick="updateStatus(' + t.id + ', \'completed\')">Завершить</button> ';
                        actions += '<button class="btn btn-danger" onclick="reportIssue(' + t.id + ', \'vehicle_broken\')">Поломка</button>';
                    }
                    return '<tr>' +
                        '<td>' + (i + 1) + '</td>' +
                        '<td>' + (t.route_number || t.routeNumber || '—') + '</td>' +
                        '<td>' + (t.departure_time || t.departureTime || '—') + '</td>' +
                        '<td>' + (t.vehicle_reg || t.vehicleReg || '—') + '</td>' +
                        '<td>' + t.status + '</td>' +
                        '<td>' + (actions || '—') + '</td>' +
                    '</tr>';
                }).join('');
        });
}

function updateStatus(id, status) {
    var body = { status: status };
    if (status === 'in_progress') {
        body.actualDeparture = new Date().toISOString();
        var mileage = prompt('Пробег при выезде (км) — обязательно для контроля:');
        if (!mileage) { showToast('Укажите пробег при выезде', true); return; }
        body.startMileage = parseInt(mileage);
    }
    if (status === 'completed') {
        body.actualArrival = new Date().toISOString();
        var mileage = prompt('Пробег при возврате (км) — обязательно для контроля:');
        if (!mileage) { showToast('Укажите пробег при возврате', true); return; }
        body.endMileage = parseInt(mileage);
    }

    fetch('/api/trips/' + id + '/status', {
        method: 'PATCH',
        headers: tripRequestHeaders(),
        body: JSON.stringify(body)
    }).then(res => {
        if (res.ok) { loadSchedule(); showToast('Статус обновлён'); }
        else res.json().then(err => showToast(err.message || 'Ошибка', true));
    });
}

function reportIssue(id, issueType) {
    var msg = issueType === 'vehicle_broken' ? 'Машина сломалась?' : 'Вы заболели?';
    if (!confirm(msg)) return;

    fetch('/api/trips/' + id + '/status', {
        method: 'PATCH',
        headers: tripRequestHeaders(),
        body: JSON.stringify({ status: 'cancelled', details: issueType, reportedBy: localStorage.getItem('username') || 'водитель' })
    }).then(res => {
        if (res.ok) { loadSchedule(); showToast('Диспетчер уведомлён — уведомление отправлено'); }
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