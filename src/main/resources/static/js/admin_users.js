var modal;
var allUsers = [];

// ЗАГРУЗКА ДАННЫХ
// Загружает список всех пользователей с сервера
function loadUsers() {
    fetch('/api/admin/users')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            allUsers = data;
            filterUsers();
        });
}

// ФИЛЬТРАЦИЯ И ОТОБРАЖЕНИЕ
// Фильтрует пользователей по поиску и роли, группирует по ролям
function filterUsers() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var roleFilter = document.getElementById('roleFilter').value;

    var filtered = allUsers.filter(function(u) {
        var matchSearch = !search || u.username.toLowerCase().indexOf(search) !== -1;
        var matchRole = roleFilter === 'all' || u.roleName === roleFilter;
        return matchSearch && matchRole;
    });

    var byRole = {};
    filtered.forEach(function(u) {
        var role = u.roleName || 'Без роли';
        if (!byRole[role]) byRole[role] = [];
        byRole[role].push(u);
    });

    var container = document.getElementById('tablesContainer');
    var html = '';
    for (var role in byRole) {
        html += '<h3>' + role + '</h3>';
        html += '<table class="table"><thead><tr><th>№</th><th>Логин</th><th>Привязан к водителю</th><th>Действия</th></tr></thead><tbody>';
        html += byRole[role].map(function(u, i) {
            return '<tr><td>' + (i + 1) + '</td><td>' + u.username + '</td><td>' + (u.driverName || '—') + '</td><td>' +
                '<button class="btn btn-sm btn-warning" onclick="openEditUser(' + u.id + ', \'' + u.username + '\', ' + u.roleId + ', ' + (u.driverId || 'null') + ')">Изменить</button> ' +
                '<button class="btn btn-sm btn-danger" onclick="deleteUser(' + u.id + ')">Удалить</button>' +
            '</td></tr>';
        }).join('');
        html += '</tbody></table>';
    }
    container.innerHTML = html || '<p class="empty-state">Нет пользователей</p>';
}

// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// Загружает список водителей для выпадающего списка
function loadDriversForSelect(selectedId) {
    fetch('/api/drivers')
        .then(function(r) { return r.json(); })
        .then(function(drivers) {
            var select = document.getElementById('driverId');
            select.innerHTML = '<option value="">Без привязки</option>' +
                drivers.map(function(d) {
                    return '<option value="' + d.id + '"' + (d.id === selectedId ? ' selected' : '') + '>' + d.fullName + '</option>';
                }).join('');
        });
}

// УПРАВЛЕНИЕ МОДАЛЬНЫМ ОКНОМ
// Открывает модальное окно для добавления нового пользователя
function openAddUser() {
    document.getElementById('modalTitle').textContent = 'Добавить пользователя';
    document.getElementById('editId').value = '';
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('password').disabled = false;
    document.getElementById('roleId').value = '1';
    loadDriversForSelect(null);
    modal.show();
}

// Открывает модальное окно для редактирования пользователя
function openEditUser(id, username, roleId, driverId) {
    document.getElementById('modalTitle').textContent = 'Изменить пользователя';
    document.getElementById('editId').value = id;
    document.getElementById('username').value = username;
    document.getElementById('password').value = '';
    document.getElementById('password').disabled = false;
    document.getElementById('password').placeholder = 'Оставьте пустым, чтобы не менять';
    document.getElementById('roleId').value = roleId;
    loadDriversForSelect(driverId !== 'null' ? driverId : null);
    modal.show();
}

// CRUD ОПЕРАЦИИ
// Сохраняет пользователя (добавляет или обновляет)
function saveUser() {
    var id = document.getElementById('editId').value;
    var password = document.getElementById('password').value;
    var body = {
        username: document.getElementById('username').value,
        roleId: parseInt(document.getElementById('roleId').value),
        driverId: document.getElementById('driverId').value ? parseInt(document.getElementById('driverId').value) : null
    };
    if (!id) {
        body.password = password;
    } else if (password) {
        body.password = password;
    }

    var url = id ? '/api/admin/users/' + id : '/api/admin/users';
    var method = id ? 'PUT' : 'POST';

    fetch(url, { method: method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
        .then(function(res) {
            if (res.ok) { modal.hide(); loadUsers(); showToast(id ? 'Пользователь обновлён' : 'Пользователь создан'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
        });
}

// Удаляет пользователя по ID
function deleteUser(id) {
    if (!confirm('Удалить пользователя?')) return;
    fetch('/api/admin/users/' + id, { method: 'DELETE' })
        .then(function(res) {
            if (res.ok) { loadUsers(); showToast('Пользователь удалён'); }
            else { return res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
        });
}



// ИНИЦИАЛИЗАЦИЯ
window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    loadUsers();
};