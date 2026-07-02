var modal;
var detailsModal;
var addStopModal;
var allRoutes = [];
var currentRouteId;

// ЗАГРУЗКА ДАННЫХ
// Загружает список всех маршрутов с сервера
function loadRoutes() {
    fetch('/api/routes')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            allRoutes = data;
            filterRoutes();
        });
}

// ФИЛЬТРАЦИЯ И ОТОБРАЖЕНИЕ
// Фильтрует маршруты по поиску и типу транспорта, группирует по типам
function filterRoutes() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var typeFilter = document.getElementById('typeFilter').value;

    var filtered = allRoutes.filter(function(r) {
        var matchSearch = !search ||
            r.number.toLowerCase().indexOf(search) !== -1 ||
            r.name.toLowerCase().indexOf(search) !== -1;
        var matchType = typeFilter === 'all' || r.transportTypeName === typeFilter;
        return matchSearch && matchType;
    });

    var byType = {};
    filtered.forEach(function(r) {
        var type = r.transportTypeName || 'Без типа';
        if (!byType[type]) byType[type] = [];
        byType[type].push(r);
    });

    var container = document.getElementById('tablesContainer');
    var html = '';
    for (var type in byType) {
        html += '<h3>' + type + '</h3>';
        html += '<table class="table"><thead><tr><th>№</th><th>Номер</th><th>Название</th><th>Длина (км)</th><th>Время (мин)</th><th>Действия</th></tr></thead><tbody>';
        html += byType[type].map(function(r, i) {
        return '<tr><td>' + (i + 1) + '</td><td>' + r.number + '</td><td>' + r.name + '</td><td>' + (r.lengthKm || '—') + '</td><td>' + (r.durationMin || '—') + '</td><td>' +
        '<button class="btn btn-sm btn-info" onclick="viewRouteDetails(' + r.id + ')">Детали</button> ' +
        '<button class="btn btn-sm btn-warning" onclick="openEditRoute(' + r.id + ', \'' + r.number + '\', \'' + r.name.replace(/'/g, "\\'") + '\', ' + r.transportTypeId + ')">Изменить</button> ' +
        '<button class="btn btn-sm btn-danger" onclick="deleteRoute(' + r.id + ')">Удалить</button>' +
        '</td></tr>';
        }).join('');
        html += '</tbody></table>';
    }
    container.innerHTML = html || '<p class="empty-state">Нет маршрутов</p>';
}

// ДЕТАЛИ МАРШРУТА
// Показывает модальное окно с деталями маршрута
function viewRouteDetails(routeId) {
    currentRouteId = routeId;
    fetch('/api/routes/' + routeId)
        .then(function(r) { return r.json(); })
        .then(function(route) {
            document.getElementById('detNumber').textContent = route.number;
            document.getElementById('detName').textContent = route.name;
            document.getElementById('detLength').textContent = route.lengthKm || '—';
            document.getElementById('detDuration').textContent = route.durationMin || '—';
            document.getElementById('detType').textContent = route.transportTypeName || '—';
        });
    loadRouteStops(routeId);
    detailsModal.show();
}

// Загружает и отображает список остановок маршрута
function loadRouteStops(routeId) {
    fetch('/api/routes/' + routeId + '/stops')
        .then(function(r) { return r.json(); })
        .then(function(stops) {
            document.getElementById('detStops').innerHTML = stops.length === 0
                ? '<tr><td colspan="5" class="empty-state">Нет остановок</td></tr>'
                : stops.map(function(s, i) {
                    return '<tr>' +
                        '<td>' + (i + 1) + '</td>' +
                        '<td>' + s.stopName + '</td>' +
                        '<td>' + (s.distanceToNextKm || '—') + '</td>' +
                        '<td>' + (s.timeToNextMin || '—') + '</td>' +
                        '<td>' +
                            (i > 0 ? '<button class="btn btn-sm btn-outline-secondary" onclick="moveStop(' + routeId + ', ' + s.stopId + ', -1)">↑</button> ' : '') +
                            (i < stops.length - 1 ? '<button class="btn btn-sm btn-outline-secondary" onclick="moveStop(' + routeId + ', ' + s.stopId + ', 1)">↓</button> ' : '') +
                            '<button class="btn btn-sm btn-warning" onclick="editStopOnRoute(' + routeId + ', ' + s.stopId + ', ' + s.orderNum + ', ' + (s.distanceToNextKm || 'null') + ', ' + (s.timeToNextMin || 'null') + ')">✏️</button> ' +
                            '<button class="btn btn-sm btn-danger" onclick="removeStopFromRoute(' + routeId + ', ' + s.stopId + ')">×</button>' +
                        '</td>' +
                    '</tr>';
                }).join('');
        });
}

// УПРАВЛЕНИЕ ОСТАНОВКАМИ НА МАРШРУТЕ
// Перемещает остановку вверх или вниз по порядку
function moveStop(routeId, stopId, direction) {
    fetch('/api/routes/' + routeId + '/stops/' + stopId + '/move?direction=' + direction, { method: 'PUT' })
        .then(function(res) {
            if (res.ok) {
                loadRouteStops(routeId);
                showToast('Порядок изменён');
            } else {
                return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); });
            }
        });
}

// Открывает модальное окно для добавления остановки на маршрут
function showAddStopToRoute() {
    resetAddStopModal();
    document.getElementById('addStopRouteId').value = currentRouteId;
    document.getElementById('addStopOrderNum').value = '1';
    document.getElementById('addStopDistance').value = '';
    document.getElementById('addStopTime').value = '';
    loadStopsForSelect();
    addStopModal.show();
}

// Загружает список всех остановок для выпадающего списка
function loadStopsForSelect() {
    fetch('/api/stops')
        .then(function(r) { return r.json(); })
        .then(function(stops) {
            document.getElementById('addStopId').innerHTML = stops.map(function(s) {
                return '<option value="' + s.id + '">' + s.name + '</option>';
            }).join('');
        });
}

// Добавляет остановку на маршрут
function addStopToRoute() {
    var routeId = document.getElementById('addStopRouteId').value;
    var body = {
        stopId: parseInt(document.getElementById('addStopId').value),
        routeId: parseInt(routeId),
        orderNum: parseInt(document.getElementById('addStopOrderNum').value),
        distanceToNextKm: document.getElementById('addStopDistance').value ? parseFloat(document.getElementById('addStopDistance').value) : null,
        timeToNextMin: document.getElementById('addStopTime').value ? parseInt(document.getElementById('addStopTime').value) : null
    };

    fetch('/api/routes/' + routeId + '/stops', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(function(res) {
        if (res.ok) {
            resetAddStopModal();
            addStopModal.hide();
            loadRouteStops(routeId);
            showToast('Остановка добавлена');
        } else {
            return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); });
        }
    });
}

// Открывает модальное окно для редактирования остановки на маршруте
function editStopOnRoute(routeId, stopId, orderNum, distanceToNextKm, timeToNextMin) {
    document.getElementById('addStopRouteId').value = routeId;
    document.getElementById('addStopOrderNum').value = orderNum;
    document.getElementById('addStopDistance').value = distanceToNextKm !== 'null' ? distanceToNextKm : '';
    document.getElementById('addStopTime').value = timeToNextMin !== 'null' ? timeToNextMin : '';
    document.getElementById('modalTitleAddStop').textContent = 'Изменить остановку';
    document.getElementById('addStopBtn').textContent = 'Сохранить';
    document.getElementById('addStopBtn').setAttribute('onclick', 'updateStopOnRoute()');

    fetch('/api/stops')
        .then(function(r) { return r.json(); })
        .then(function(stops) {
            document.getElementById('addStopId').innerHTML = stops.map(function(s) {
                return '<option value="' + s.id + '"' + (s.id === stopId ? ' selected' : '') + '>' + s.name + '</option>';
            }).join('');
        });

    document.getElementById('addStopId').disabled = true;
    addStopModal.show();
}

// Обновляет данные остановки на маршруте
function updateStopOnRoute() {
    var routeId = document.getElementById('addStopRouteId').value;
    var stopId = parseInt(document.getElementById('addStopId').value);
    var body = {
        orderNum: parseInt(document.getElementById('addStopOrderNum').value),
        distanceToNextKm: document.getElementById('addStopDistance').value ? parseFloat(document.getElementById('addStopDistance').value) : null,
        timeToNextMin: document.getElementById('addStopTime').value ? parseInt(document.getElementById('addStopTime').value) : null
    };

    fetch('/api/routes/' + routeId + '/stops/' + stopId, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(function(res) {
        if (res.ok) {
            resetAddStopModal();
            addStopModal.hide();
            loadRouteStops(routeId);
            showToast('Остановка обновлена');
        } else {
            return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); });
        }
    });
}

// Сбрасывает форму добавления остановки
function resetAddStopModal() {
    document.getElementById('addStopId').disabled = false;
    document.getElementById('modalTitleAddStop').textContent = 'Добавить остановку на маршрут';
    document.getElementById('addStopBtn').textContent = 'Добавить';
    document.getElementById('addStopBtn').setAttribute('onclick', 'addStopToRoute()');
}

// Удаляет остановку с маршрута
function removeStopFromRoute(routeId, stopId) {
    if (!confirm('Удалить остановку с маршрута?')) return;
    fetch('/api/routes/' + routeId + '/stops/' + stopId, { method: 'DELETE' })
        .then(function(res) {
            if (res.ok) { loadRouteStops(routeId); showToast('Остановка удалена'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
        });
}

// УПРАВЛЕНИЕ МОДАЛЬНЫМ ОКНОМ МАРШРУТА
// Открывает модальное окно для добавления нового маршрута
function openAddRoute() {
    document.getElementById('modalTitle').textContent = 'Добавить маршрут';
    document.getElementById('editId').value = '';
    document.getElementById('number').value = '';
    document.getElementById('name').value = '';
    document.getElementById('transportTypeId').value = '1';
    modal.show();
}

// Открывает модальное окно для редактирования маршрута
function openEditRoute(id, number, name, transportTypeId) {
    document.getElementById('modalTitle').textContent = 'Изменить маршрут';
    document.getElementById('editId').value = id;
    document.getElementById('number').value = number;
    document.getElementById('name').value = name;
    document.getElementById('transportTypeId').value = transportTypeId;
    modal.show();
}

// CRUD ОПЕРАЦИИ
// Сохраняет маршрут (добавляет или обновляет)
function saveRoute() {
    var id = document.getElementById('editId').value;
    var body = {
        number: document.getElementById('number').value,
        name: document.getElementById('name').value,
        transportTypeId: parseInt(document.getElementById('transportTypeId').value)
    };
    var url = id ? '/api/admin/routes/' + id : '/api/admin/routes';
    var method = id ? 'PUT' : 'POST';

    fetch(url, { method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
        .then(function(res) {
            if (res.ok) { modal.hide(); loadRoutes(); showToast(id ? 'Маршрут обновлён' : 'Маршрут добавлен'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
        });
}

// Удаляет маршрут по ID
function deleteRoute(id) {
    if (!confirm('Удалить маршрут?')) return;
    fetch('/api/admin/routes/' + id, { method: 'DELETE' })
        .then(function(res) {
            if (res.ok) { loadRoutes(); showToast('Маршрут удалён'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Невозможно удалить', true); }); }
        });
}

// ИНИЦИАЛИЗАЦИЯ
window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    detailsModal = new bootstrap.Modal(document.getElementById('detailsModal'));
    addStopModal = new bootstrap.Modal(document.getElementById('addStopModal'));
    loadRoutes();
};