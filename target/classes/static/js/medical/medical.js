var today = new Date().toISOString().split('T')[0];

window.onload = function() {
    loadDrivers();
    loadHistory();
};

function loadDrivers() {
    fetch('/api/dispatcher/trips?date=' + today)
        .then(r => r.json())
        .then(trips => {
            var driverIds = [...new Set(trips.map(t => t.driver_id).filter(id => id))];
            return fetch('/api/drivers').then(r => r.json()).then(drivers => {
                document.getElementById('medDriver').innerHTML = drivers
                    .filter(d => driverIds.includes(d.id))
                    .map(d => '<option value="' + d.id + '">' + d.fullName + '</option>').join('');
            });
        });
}

function loadHistory() {
    fetch('/api/admin/medical?date=' + today)
        .then(r => r.json())
        .then(data => {
            document.getElementById('medHistory').innerHTML = data.length === 0
                ? '<tr><td colspan="4">Нет данных</td></tr>'
                : data.map(m => {
                    var dt = m.created_at ? new Date(m.created_at).toLocaleString() : m.check_date;
                    return '<tr><td>' + m.driver_name + '</td><td>' + m.check_type + '</td><td>' + m.result + '</td><td>' + dt + '</td></tr>';
                }).join('');
        });
}

function submitMedical() {
    fetch('/api/medical/drivers/' + document.getElementById('medDriver').value + '/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            checkType: document.getElementById('medType').value,
            result: document.getElementById('medResult').value,
            notes: document.getElementById('medNotes').value,
            checkedBy: 'Медик'
        })
    }).then(r => {
        if (r.ok) { alert('Сохранено'); loadHistory(); }
        else r.json().then(e => alert(e.message));
    });
}