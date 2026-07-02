var modal;
var allStops = [];

// ЗАГРУЗКА ДАННЫХ
// Загружает список всех остановок с сервера
function loadStops() {
    fetch('/api/stops')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            allStops = data;
            filterStops();
        });
}

// ФИЛЬТРАЦИЯ И ОТОБРАЖЕНИЕ
// Фильтрует остановки по поиску и отображает в таблице
function filterStops() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var filtered = allStops.filter(function(s) {
        return !search || s.name.toLowerCase().indexOf(search) !== -1;
    });

    var tbody = document.getElementById('tableBody');
    if (!tbody) return;
    tbody.innerHTML = filtered.length === 0
        ? '<tr><td colspan="6" class="empty-state">Нет остановок</td></tr>'
        : filtered.map(function(s, index) {
            return '<tr>' +
                '<td>' + (index + 1) + '</td>' +
                '<td>' + s.name + '</td>' +
                '<td>' + s.latitude + '</td>' +
                '<td>' + s.longitude + '</td>' +
                '<td>' + s.stopType + '</td>' +
                '<td>' +
                    '<button class="btn btn-sm btn-warning" onclick="openEditStop(' + s.id + ', \'' + s.name.replace(/'/g, "\\'") + '\', ' + s.latitude + ', ' + s.longitude + ', \'' + s.stopType + '\')">Изменить</button> ' +
                    '<button class="btn btn-sm btn-danger" onclick="deleteStop(' + s.id + ')">Удалить</button>' +
                '</td>' +
            '</tr>';
        }).join('');
}

// УПРАВЛЕНИЕ МОДАЛЬНЫМ ОКНОМ
// Открывает модальное окно для добавления новой остановки
function openAddStop() {
    document.getElementById('modalTitle').textContent = 'Добавить остановку';
    document.getElementById('editId').value = '';
    document.getElementById('name').value = '';
    document.getElementById('latitude').value = '55.0';
    document.getElementById('longitude').value = '37.0';
    document.getElementById('stopType').value = 'intermediate';
    modal.show();
}

// Открывает модальное окно для редактирования остановки
function openEditStop(id, name, latitude, longitude, stopType) {
    document.getElementById('modalTitle').textContent = 'Изменить остановку';
    document.getElementById('editId').value = id;
    document.getElementById('name').value = name;
    document.getElementById('latitude').value = latitude;
    document.getElementById('longitude').value = longitude;
    document.getElementById('stopType').value = stopType;
    modal.show();
}

// CRUD ОПЕРАЦИИ
// Сохраняет остановку (добавляет или обновляет)
function saveStop() {
    var id = document.getElementById('editId').value;
    var body = {
        name: document.getElementById('name').value,
        latitude: parseFloat(document.getElementById('latitude').value),
        longitude: parseFloat(document.getElementById('longitude').value),
        stopType: document.getElementById('stopType').value
    };
    var url = id ? '/api/admin/stops/' + id : '/api/admin/stops';
    var method = id ? 'PUT' : 'POST';

    fetch(url, { method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
        .then(function(res) {
            if (res.ok) { modal.hide(); loadStops(); showToast(id ? 'Остановка обновлена' : 'Остановка добавлена'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
        });
}

// Удаляет остановку по ID
function deleteStop(id) {
    if (!confirm('Удалить остановку?')) return;
    fetch('/api/admin/stops/' + id, { method: 'DELETE' })
        .then(function(res) {
            if (res.ok) { loadStops(); showToast('Остановка удалена'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Невозможно удалить', true); }); }
        });
}

// ИНИЦИАЛИЗАЦИЯ
window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    loadStops();
};