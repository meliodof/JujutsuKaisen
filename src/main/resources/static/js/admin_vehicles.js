var modal;
var allVehicles = [];

// ВАЛИДАЦИЯ И ФОРМАТИРОВАНИЕ
// Форматирует ввод номера в реальном времени
function formatRegNumber(input) {
    var value = input.value.toUpperCase().replace(/[^А-ЯA-Z0-9]/g, '');

    var letter1 = value.substring(0, 1).replace(/[^А-ЯA-Z]/g, '');
    var digits = value.substring(1, 4).replace(/[^0-9]/g, '');
    var letters2 = value.substring(4, 6).replace(/[^А-ЯA-Z]/g, '');
    var region = value.substring(6, 9).replace(/[^0-9]/g, '');

    var result = letter1 + digits + letters2;
    if (region) result += ' ' + region;

    input.value = result;
    validateRegNumber();
}

// Проверяет корректность формата номера
function validateRegNumber() {
    var input = document.getElementById('regNumber');
    var value = input.value.trim();
    var errorDiv = document.getElementById('regNumberError');

    var validFormat = /^[А-ЯA-Z]\d{3}[А-ЯA-Z]{2}(\s\d{2,3})?$/i;

    if (value.length === 0) {
        errorDiv.textContent = '';
        input.style.borderColor = '';
        return;
    }

    if (!validFormat.test(value)) {
        errorDiv.textContent = 'Неверный формат. Правильный формат: A111AA или A111AA 163';
        input.style.borderColor = '#d32f2f';
    } else {
        errorDiv.textContent = '';
        input.style.borderColor = '#2e7d32';
    }
}

// ЗАГРУЗКА ДАННЫХ
// Загружает модели транспорта с сервера
function loadModels(selectedId) {
    fetch('/api/vehicle-models')
        .then(function(r) { return r.json(); })
        .then(function(models) {
            var select = document.getElementById('modelId');
            select.innerHTML = models.map(function(m) {
                return '<option value="' + m.id + '" data-type="' + (m.transport_type_name || '') + '"' + (m.id === selectedId ? ' selected' : '') + '>' + m.name + ' (вмест. ' + m.default_capacity + ')</option>';
            }).join('');
            updateTransportType();
        });
}

// Загружает список всех машин с сервера
function loadVehicles() {
    fetch('/api/vehicles')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            allVehicles = data;
            filterVehicles();
        });
}

// ФИЛЬТРАЦИЯ И ОТОБРАЖЕНИЕ
// Фильтрует машины по поиску и статусу, разносит по трем таблицам
function filterVehicles() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var statusFilter = document.getElementById('statusFilter').value;

    var filtered = allVehicles.filter(function(v) {
        var matchSearch = !search ||
            v.regNumber.toLowerCase().indexOf(search) !== -1 ||
            (v.modelName && v.modelName.toLowerCase().indexOf(search) !== -1);
        var matchStatus = statusFilter === 'all' || v.status === statusFilter;
        return matchSearch && matchStatus;
    });

    var active = filtered.filter(function(v) { return v.status === 'active'; });
    var maintenance = filtered.filter(function(v) { return v.status === 'maintenance'; });
    var repair = filtered.filter(function(v) { return v.status === 'repair'; });

    function renderTable(tbodyId, items) {
        var tbody = document.getElementById(tbodyId);
        if (!tbody) return;
        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="empty-state">Нет машин</td></tr>';
            return;
        }
        tbody.innerHTML = items.map(function(v, index) {
            return '<tr>' +
                '<td>' + (index + 1) + '</td>' +
                '<td>' + v.regNumber + '</td>' +
                '<td>' + (v.modelName || 'ID: ' + v.modelId) + '</td>' +
                '<td>' + v.capacity + '</td>' +
                '<td>' + (v.transportTypeName || '—') + '</td>' +
               '<td>' +
                   '<button class="btn btn-sm btn-warning" onclick="openEditVehicle(' + v.id + ', \'' + v.regNumber + '\', ' + v.modelId + ', \'' + v.status + '\')">Изменить</button> ' +
                   '<button class="btn btn-sm btn-info" onclick="openMaintenanceModal(' + v.id + ', \'' + v.regNumber + '\')">ТО</button> ' +
                   '<button class="btn btn-sm btn-danger" onclick="deleteVehicle(' + v.id + ')">Удалить</button>' +
               '</td>';
        }).join('');
    }

    renderTable('tableBodyActive', active);
    renderTable('tableBodyMaintenance', maintenance);
    renderTable('tableBodyRepair', repair);
}

// УПРАВЛЕНИЕ МОДАЛЬНЫМ ОКНОМ
// Открывает модальное окно для добавления новой машины
function openAddVehicle() {
    document.getElementById('modalTitle').textContent = 'Добавить машину';
    document.getElementById('editId').value = '';
    document.getElementById('regNumber').value = '';
    document.getElementById('status').value = 'active';
    document.getElementById('regNumberError').textContent = '';
    document.getElementById('regNumber').style.borderColor = '';
    loadModels(null);
    modal.show();
}

// Открывает модальное окно для редактирования машины
function openEditVehicle(id, regNumber, modelId, status) {
    document.getElementById('modalTitle').textContent = 'Изменить машину';
    document.getElementById('editId').value = id;
    document.getElementById('regNumber').value = regNumber;
    document.getElementById('status').value = status;
    document.getElementById('regNumberError').textContent = '';
    document.getElementById('regNumber').style.borderColor = '';
    loadModels(modelId);
    modal.show();
}

// CRUD ОПЕРАЦИИ
// Сохраняет машину (добавляет или обновляет)
function saveVehicle() {
    var regNumber = document.getElementById('regNumber').value.trim();

    var validFormat = /^[А-ЯA-Z]\d{3}[А-ЯA-Z]{2}(\s\d{2,3})?$/i;
    if (!validFormat.test(regNumber)) {
      showToast('Неверный формат номера', true);
      return;
    }

    var id = document.getElementById('editId').value;
    var body = {
        regNumber: regNumber,
        modelId: parseInt(document.getElementById('modelId').value),
        status: document.getElementById('status').value
    };
    var url = id ? '/api/admin/vehicles/' + id : '/api/admin/vehicles';
    var method = id ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(function(res) {
        if (res.ok) {
            modal.hide();
            loadVehicles();
            showToast(id ? 'Машина обновлена' : 'Машина добавлена');
        } else {
            return res.json().then(function(err) {
                showToast(err.message || 'Ошибка', true);
            });
        }
    });
}

// Удаляет машину по ID
function deleteVehicle(id) {
    if (!confirm('Удалить машину?')) return;
    fetch('/api/admin/vehicles/' + id, { method: 'DELETE' })
        .then(function(res) {
            if (res.ok) {
                loadVehicles();
                showToast('Машина удалена');
            } else {
                return res.json().then(function(err) {
                    showToast(err.message || 'Невозможно удалить', true);
                });
            }
        });
}

// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// Обновляет поле типа транспорта при выборе модели
function updateTransportType() {
    var select = document.getElementById('modelId');
    var option = select.options[select.selectedIndex];
    var type = option.getAttribute('data-type') || '—';
    document.getElementById('transportType').value = type;
}

var maintenanceModal;

function openMaintenanceModal(vehicleId, regNumber) {
    document.getElementById('maintVehicleId').value = vehicleId;
    document.getElementById('maintTitle').textContent = 'ТО — ' + regNumber;
    fetch('/api/vehicles/' + vehicleId + '/maintenance')
        .then(function(r) { return r.json(); })
        .then(function(d) {
            document.getElementById('lastServiceMileage').value = d.last_service_mileage || 0;
            document.getElementById('serviceIntervalKm').value = d.service_interval_km || 15000;
            document.getElementById('maintInfo').textContent =
                'Текущий пробег: ' + (d.currentMileage || '—') + ' км. До ТО: ' + (d.kmUntilService != null ? d.kmUntilService : '—') + ' км'
                + (d.due ? ' (ТРЕБУЕТСЯ ТО!)' : '');
            maintenanceModal.show();
        })
        .catch(function() { showToast('Выполните migration_notifications_maintenance.sql', true); });
}

function saveMaintenanceSettings() {
    var id = document.getElementById('maintVehicleId').value;
    fetch('/api/vehicles/' + id + '/maintenance', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            lastServiceMileage: parseInt(document.getElementById('lastServiceMileage').value),
            serviceIntervalKm: parseInt(document.getElementById('serviceIntervalKm').value)
        })
    }).then(function(r) {
        if (r.ok) { maintenanceModal.hide(); showToast('Настройки ТО сохранены'); }
        else r.json().then(function(e) { showToast(e.message || 'Ошибка', true); });
    });
}

function completeMaintenance() {
    var id = document.getElementById('maintVehicleId').value;
    if (!confirm('Отметить ТО выполненным на текущем пробеге?')) return;
    fetch('/api/vehicles/' + id + '/maintenance/complete', { method: 'POST' })
        .then(function(r) {
            if (r.ok) { maintenanceModal.hide(); showToast('ТО отмечено выполненным'); }
            else r.json().then(function(e) { showToast(e.message || 'Ошибка', true); });
        });
}

// ИНИЦИАЛИЗАЦИЯ
window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    if (document.getElementById('maintenanceModal')) {
        maintenanceModal = new bootstrap.Modal(document.getElementById('maintenanceModal'));
    }
    loadVehicles();
};