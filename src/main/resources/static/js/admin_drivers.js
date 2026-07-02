var modal;
var driverDetailsModal;
var allDrivers = [];

function formatPhone(input) {
    var value = input.value.replace(/[^\d]/g, '');
    var result = '+7';
    if (value.length > 1) result += ' (' + value.substring(1, 4);
    if (value.length >= 4) result += ') ' + value.substring(4, 7);
    if (value.length >= 7) result += '-' + value.substring(7, 9);
    if (value.length >= 9) result += '-' + value.substring(9, 11);
    input.value = result;
}

function formatLicense(input) {
    var value = input.value.replace(/[^\d]/g, '');
    if (value.length > 4) value = value.substring(0, 4) + ' ' + value.substring(4, 10);
    input.value = value;
}

function updateFullName() {
    var lastName = document.getElementById('lastName').value.trim();
    var firstName = document.getElementById('firstName').value.trim();
    var middleName = document.getElementById('middleName').value.trim();
    var fullName = lastName + ' ' + firstName;
    if (middleName) fullName += ' ' + middleName;
    document.getElementById('fullName').value = fullName;
}

function parseFullName(fullName) {
    if (!fullName) return;
    var parts = fullName.split(' ');
    document.getElementById('lastName').value = parts[0] || '';
    document.getElementById('firstName').value = parts[1] || '';
    document.getElementById('middleName').value = parts.slice(2).join(' ') || '';
}

function clearErrors() {
    ['lastNameError', 'firstNameError', 'phoneError', 'passportSeriesError',
     'passportNumberError', 'passportIssuedError', 'passportDateError',
     'licenseNumberError', 'licenseDateError'].forEach(function(id) {
        document.getElementById(id).textContent = '';
    });
}

function onlyLetters(input) {
    input.value = input.value.replace(/[^а-яА-ЯёЁa-zA-Z\s\-]/g, '');
}

function onlyLettersAndSpaces(input) {
    input.value = input.value.replace(/[^а-яА-ЯёЁa-zA-Z\s]/g, '');
}

function loadDrivers() {
    fetch('/api/drivers')
        .then(function(r) { return r.json(); })
        .then(function(data) { allDrivers = data; filterDrivers(); });
}

function filterDrivers() {
    var search = document.getElementById('searchInput').value.toLowerCase();
    var categoryFilter = document.getElementById('categoryFilter').value;
    var filtered = allDrivers.filter(function(d) {
        return (!search || d.fullName.toLowerCase().indexOf(search) !== -1 || d.phone.indexOf(search) !== -1) &&
               (categoryFilter === 'all' || d.licenseCategory === categoryFilter);
    });
    var categoryD = filtered.filter(function(d) { return d.licenseCategory === 'D'; });
    var categoryDE = filtered.filter(function(d) { return d.licenseCategory === 'DE'; });
    var other = filtered.filter(function(d) { return d.licenseCategory !== 'D' && d.licenseCategory !== 'DE'; });

    function render(tbodyId, items, showCategory) {
        var tbody = document.getElementById(tbodyId);
        if (!tbody) return;
        if (items.length === 0) { tbody.innerHTML = '<tr><td colspan="' + (showCategory ? 6 : 5) + '" class="empty-state">Нет водителей</td></tr>'; return; }
        tbody.innerHTML = items.map(function(d, i) {
            var row = '<tr><td>' + (i+1) + '</td><td>' + d.fullName + '</td><td>' + d.experienceYears + '</td>';
            if (showCategory) row += '<td>' + d.licenseCategory + '</td>';
            row += '<td>' + d.phone + '</td><td>' +
                '<button class="btn btn-sm btn-info" onclick="viewDriverDetails(' + d.id + ')">Детали</button> ' +
                '<button class="btn btn-sm btn-warning" onclick="openEditDriver(' + d.id + ', \'' + d.fullName.replace(/'/g, "\\'") + '\', ' + d.experienceYears + ', \'' + d.licenseCategory + '\', \'' + d.phone + '\', \'' + (d.passportSeries || '') + '\', \'' + (d.passportNumber || '') + '\', \'' + (d.passportIssued || '').replace(/'/g, "\\'") + '\', \'' + (d.passportDate || '') + '\', \'' + (d.licenseNumber || '') + '\', \'' + (d.licenseDate || '') + '\')">Изменить</button> ' +
                '<button class="btn btn-sm btn-danger" onclick="deleteDriver(' + d.id + ')">Удалить</button>' +
            '</td></tr>';
            return row;
        }).join('');
    }
    render('tableBodyD', categoryD, false);
    render('tableBodyDE', categoryDE, false);
    render('tableBodyOther', other, true);
}

function openAddDriver() {
    document.getElementById('searchInput').value = '';
    filterDrivers();
    document.getElementById('modalTitle').textContent = 'Добавить водителя';
    document.getElementById('editId').value = '';
    ['lastName','firstName','middleName','fullName','passportSeries','passportNumber','passportIssued','passportDate','licenseNumber','licenseDate'].forEach(function(id) {
        document.getElementById(id).value = '';
    });
    document.getElementById('experienceYears').value = '0';
    document.getElementById('licenseCategory').value = 'D';
    document.getElementById('phone').value = '+7';
    var today = new Date().toISOString().split('T')[0];
    document.getElementById('passportDate').setAttribute('max', today);
    document.getElementById('licenseDate').setAttribute('max', today);
    clearErrors();
    modal.show();
}

function openEditDriver(id, fullName, experienceYears, licenseCategory, phone,
                        passportSeries, passportNumber, passportIssued, passportDate,
                        licenseNumber, licenseDate) {
    document.getElementById('searchInput').value = '';
    filterDrivers();
    document.getElementById('modalTitle').textContent = 'Изменить водителя';
    document.getElementById('editId').value = id;
    parseFullName(fullName);
    document.getElementById('experienceYears').value = experienceYears;
    document.getElementById('licenseCategory').value = licenseCategory;
    document.getElementById('phone').value = phone;
    document.getElementById('passportSeries').value = passportSeries || '';
    document.getElementById('passportNumber').value = passportNumber || '';
    document.getElementById('passportIssued').value = passportIssued || '';
    document.getElementById('passportDate').value = passportDate || '';
    document.getElementById('licenseNumber').value = licenseNumber || '';
    document.getElementById('licenseDate').value = licenseDate || '';
    var today = new Date().toISOString().split('T')[0];
    document.getElementById('passportDate').setAttribute('max', today);
    document.getElementById('licenseDate').setAttribute('max', today);
    clearErrors();
    modal.show();
}

function viewDriverDetails(id) {
    fetch('/api/drivers/' + id).then(r => r.json()).then(function(d) {
        document.getElementById('detFullName').textContent = d.fullName;
        document.getElementById('detPhone').textContent = d.phone;
        document.getElementById('detExp').textContent = d.experienceYears;
        document.getElementById('detCat').textContent = d.licenseCategory;
        document.getElementById('detPassport').textContent = (d.passportSeries || '—') + ' ' + (d.passportNumber || '');
        document.getElementById('detPassportIssued').textContent = d.passportIssued || '—';
        document.getElementById('detPassportDate').textContent = d.passportDate || '—';
        document.getElementById('detLicense').textContent = d.licenseNumber || '—';
        document.getElementById('detLicenseDate').textContent = d.licenseDate || '—';
        document.getElementById('detPhoto').src = d.photoPath || '';
    });
    driverDetailsModal.show();
}

function saveDriver() {
    clearErrors();
    var errors = [];
    updateFullName();
    var lastName = document.getElementById('lastName').value.trim();
    var firstName = document.getElementById('firstName').value.trim();
    if (!lastName) errors.push({ field: 'lastNameError', message: 'Фамилия обязательна' });
    if (!firstName) errors.push({ field: 'firstNameError', message: 'Имя обязательно' });
    var phone = document.getElementById('phone').value.trim();
    if (phone.length < 16) errors.push({ field: 'phoneError', message: 'Неверный формат телефона' });
    var ps = document.getElementById('passportSeries').value.trim();
    if (ps && ps.length < 4) errors.push({ field: 'passportSeriesError', message: 'Должно быть 4 цифры' });
    var pn = document.getElementById('passportNumber').value.trim();
    if (pn && pn.length < 6) errors.push({ field: 'passportNumberError', message: 'Должно быть 6 цифр' });
    var pd = document.getElementById('passportDate').value;
    var today = new Date().toISOString().split('T')[0];
    if (pd && pd > today) errors.push({ field: 'passportDateError', message: 'Дата не может быть в будущем' });
    var ln = document.getElementById('licenseNumber').value.trim();
    if (ln && ln.replace(/\s/g, '').length < 10) errors.push({ field: 'licenseNumberError', message: 'Неверный формат В/У' });
    var ld = document.getElementById('licenseDate').value;
    if (ld && ld > today) errors.push({ field: 'licenseDateError', message: 'Дата не может быть в будущем' });
    if (errors.length > 0) { errors.forEach(function(e) { document.getElementById(e.field).textContent = e.message; }); return; }

    var id = document.getElementById('editId').value;
    var body = {
        fullName: document.getElementById('fullName').value,
        experienceYears: parseInt(document.getElementById('experienceYears').value) || 0,
        licenseCategory: document.getElementById('licenseCategory').value,
        phone: phone, passportSeries: ps || null, passportNumber: pn || null,
        passportIssued: document.getElementById('passportIssued').value.trim() || null,
        passportDate: pd || null, licenseNumber: ln || null, licenseDate: ld || null
    };
    fetch(id ? '/api/admin/drivers/' + id : '/api/admin/drivers', {
        method: id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    }).then(function(res) {
        if (res.ok) { modal.hide(); setTimeout(loadDrivers, 300); showToast(id ? 'Водитель обновлён' : 'Водитель добавлен'); }
        else { res.json().then(function(err) { showToast(err.message || 'Ошибка', true); }); }
    });
}

function deleteDriver(id) {
    if (!confirm('Удалить водителя?')) return;
    fetch('/api/admin/drivers/' + id, { method: 'DELETE' }).then(function(res) {
        if (res.ok) { setTimeout(loadDrivers, 300); showToast('Водитель удалён'); }
        else { res.json().then(function(err) { showToast(err.message || 'Невозможно удалить', true); }); }
    });
}

window.onload = function() {
    modal = new bootstrap.Modal(document.getElementById('editModal'));
    driverDetailsModal = new bootstrap.Modal(document.getElementById('driverDetailsModal'));
    loadDrivers();
};