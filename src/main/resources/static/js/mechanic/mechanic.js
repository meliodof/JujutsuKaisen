var today = new Date().toISOString().split('T')[0];

Promise.all([
    fetch('/api/dispatcher/trips?date=' + today).then(r => r.json()),
    fetch('/api/vehicles').then(r => r.json()),
    fetch('/api/drivers').then(r => r.json())
]).then(function(data) {
    var trips = data[0], vehicles = data[1], drivers = data[2];
    var vehicleIds = [...new Set(trips.map(t => t.vehicle_id).filter(id => id))];
    var driverIds = [...new Set(trips.map(t => t.driver_id).filter(id => id))];

    document.getElementById('inspVehicle').innerHTML = vehicles
        .filter(v => vehicleIds.includes(v.id))
        .map(v => '<option value="' + v.id + '">' + v.regNumber + '</option>').join('');
    document.getElementById('fuelVehicle').innerHTML = vehicles
        .filter(v => vehicleIds.includes(v.id))
        .map(v => '<option value="' + v.id + '">' + v.regNumber + '</option>').join('');
    ['inspDriver', 'fuelDriver'].forEach(function(id) {
        document.getElementById(id).innerHTML = drivers
            .filter(d => driverIds.includes(d.id))
            .map(d => '<option value="' + d.id + '">' + d.fullName + '</option>').join('');
    });
    loadLastMileage('insp');
    loadLastMileage('fuel');
});

function loadLastMileage(which) {
    var vehicleId = which === 'fuel'
        ? document.getElementById('fuelVehicle').value
        : document.getElementById('inspVehicle').value;
    if (!vehicleId) return;
    fetch('/api/vehicles/' + vehicleId + '/last-mileage')
        .then(r => r.json())
        .then(d => {
            var text = d.lastMileage ? d.lastMileage + ' км' : 'нет данных';
            if (which === 'fuel') document.getElementById('fuelMileageHint').textContent = 'Последний пробег: ' + text;
            else document.getElementById('lastMileageHint').textContent = 'Последний пробег: ' + text;
        });
}

function submitInspection() {
    var vehicleId = document.getElementById('inspVehicle').value;
    var driverId = parseInt(document.getElementById('inspDriver').value);
    var type = document.getElementById('inspType').value;
    var mileage = parseInt(document.getElementById('mileage').value) || null;

    fetch('/api/mechanic/vehicles/' + vehicleId + '/inspection', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            driverId: driverId,
            inspectionType: type,
            result: document.getElementById('inspResult').value,
            notes: document.getElementById('inspNotes').value,
            checkedBy: 'Механик',
            mileage: mileage
        })
    }).then(r => {
        if (r.ok) { alert('Сохранено'); loadLastMileage('insp'); }
        else r.json().then(e => alert(e.message));
    });
}

function submitFuel() {
    var vehicleId = document.getElementById('fuelVehicle').value;
    var liters = parseFloat(document.getElementById('fuelLiters').value);
    var mileage = parseInt(document.getElementById('fuelMileage').value);

    fetch('/api/mechanic/vehicles/' + vehicleId + '/fuel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            driverId: parseInt(document.getElementById('fuelDriver').value) || null,
            liters: liters,
            mileage: mileage,
            phase: document.getElementById('fuelPhase').value
        })
    }).then(r => {
        if (r.ok) { alert('Сохранено'); loadLastMileage('fuel'); }
        else r.json().then(e => alert(e.message));
    });
}
