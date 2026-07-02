var modal;

function load() {
    var dayNames = ['', 'ПН', 'ВТ', 'СР', 'ЧТ', 'ПТ', 'СБ', 'ВС'];
    fetch('/api/schedule/all')
        .then(r => r.json())
        .then(data => {
            document.getElementById('tableBody').innerHTML = data.length === 0
                ? '<tr><td colspan="6" class="empty-state">Нет шаблонов</td></tr>'
                : data.map((s, i) => '<tr>' +
                    '<td>' + (i + 1) + '</td>' +
                    '<td>' + (s.route_number || '—') + '</td>' +
                    '<td>' + (dayNames[s.day_of_week] || s.day_of_week) + '</td>' +
                    '<td>' + (s.departure_time || '—') + '</td>' +
                    '<td>' + (s.planned_arrival || '—') + '</td>' +
                    '<td><button class="btn btn-sm btn-danger" onclick="del(' + s.id + ')">Удалить</button></td>' +
                '</tr>').join('');
        });
}

function openAdd() {
    document.getElementById('editId').value = '';
    document.getElementById('departureTime').value = '08:00';
    loadRoutes();
    modal.show();
}

function loadRoutes() {
    fetch('/api/routes')
        .then(r => r.json())
        .then(routes => {
            document.getElementById('routeId').innerHTML = routes.map(r =>
                '<option value="' + r.id + '">' + r.number + ' - ' + r.name + '</option>').join('');
        });
}

function save() {
    var body = {
        routeId: parseInt(document.getElementById('routeId').value),
        dayOfWeek: parseInt(document.getElementById('dayOfWeek').value),
        departureTime: document.getElementById('departureTime').value
    };
    fetch('/api/schedule/templates', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    }).then(res => {
        if (res.ok) { modal.hide(); load(); showToast('Шаблон добавлен'); }
        else res.json().then(err => showToast(err.message || 'Ошибка', true));
    });
}

function del(id) {
    if (!confirm('Удалить шаблон?')) return;
    fetch('/api/schedule/templates/' + id, { method: 'DELETE' })
        .then(res => {
            if (res.ok) { load(); showToast('Удалён'); }
            else res.json().then(err => showToast(err.message || 'Ошибка', true));
        });
}

window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    load();
};