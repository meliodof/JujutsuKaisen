var today = new Date().toISOString().split('T')[0];

Promise.all([
    fetch('/api/dispatcher/trips?date=' + today).then(r => r.json()),
    fetch('/api/dispatcher/waybills?date=' + today).then(r => r.json()),
    fetch('/api/dispatcher/drivers/available?date=' + today).then(r => r.json())
]).then(function(data) {
    var trips = data[0], waybills = data[1], drivers = data[2];

    document.getElementById('todayTrips').textContent = trips.length;
    document.getElementById('waybillsReady').textContent = waybills.filter(w => w.ready).length;
    document.getElementById('waybillsPending').textContent = waybills.filter(w => !w.ready).length;
    document.getElementById('availableDrivers').textContent = drivers.length;

    var scheduled = trips.filter(t => t.status === 'scheduled').length;
    var progress = trips.filter(t => t.status === 'in_progress').length;
    var completed = trips.filter(t => t.status === 'completed').length;
    var cancelled = trips.filter(t => t.status === 'cancelled').length;

    new Chart(document.getElementById('statusChart'), {
        type: 'doughnut',
        data: {
            labels: ['Запланирован', 'В пути', 'Завершён', 'Отменён'],
            datasets: [{ data: [scheduled, progress, completed, cancelled], backgroundColor: ['#3b82f6','#f59e0b','#16a34a','#ef4444'] }]
        }
    });

    var ready = waybills.filter(w => w.ready).length;
    var waitBoth = waybills.filter(w => !w.ready && !w.medical_ok && !w.inspection_ok).length;
    var waitMed = waybills.filter(w => !w.ready && !w.medical_ok && w.inspection_ok).length;
    var waitInsp = waybills.filter(w => !w.ready && w.medical_ok && !w.inspection_ok).length;

    new Chart(document.getElementById('waybillChart'), {
        type: 'bar',
        data: {
            labels: ['Готовы к выезду', 'Ждут медика и механика', 'Ждут медика', 'Ждут механика'],
            datasets: [{ label: 'Путевые листы', data: [ready, waitBoth, waitMed, waitInsp], backgroundColor: ['#16a34a','#ef4444','#f59e0b','#3b82f6'] }]
        },
        options: { scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
    });

    var last7 = [];
    var labels = [];
    for (var i = 6; i >= 0; i--) {
        var d = new Date(); d.setDate(d.getDate() - i);
        labels.push(d.toLocaleDateString('ru', { weekday: 'short', day: 'numeric' }));
        last7.push(fetch('/api/dispatcher/trips?date=' + d.toISOString().split('T')[0]).then(r => r.json()));
    }
    Promise.all(last7).then(function(days) {
        new Chart(document.getElementById('tripsChart'), {
            type: 'bar',
            data: { labels: labels, datasets: [{ label: 'Рейсов', data: days.map(d => d.length), backgroundColor: '#00529b' }] },
            options: { scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
        });
    });

    fetch('/api/dispatcher/maintenance/due')
        .then(r => r.json())
        .then(list => {
            var el = document.getElementById('maintenanceDueList');
            if (!el) return;
            el.innerHTML = list.length === 0
                ? '<li class="text-success">Все машины в норме</li>'
                : list.map(v => '<li><strong>' + v.reg_number + '</strong> — пробег '
                    + v.current_mileage + ' км (интервал ТО ' + v.service_interval_km + ' км, последнее ТО на '
                    + v.last_service_mileage + ' км)</li>').join('');
        })
        .catch(() => {
            var el = document.getElementById('maintenanceDueList');
            if (el) el.innerHTML = '<li class="text-danger">Выполните SQL из db/migration_notifications_maintenance.sql</li>';
        });
});
