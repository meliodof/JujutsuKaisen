function togglePassword() {
    var input = document.getElementById('password');
    input.type = input.type === 'password' ? 'text' : 'password';
}

async function login() {
    var username = document.getElementById('username').value;
    var password = document.getElementById('password').value;
    var errorDiv = document.getElementById('error');

    try {
        var response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, password: password })
        });

        if (!response.ok) {
            var data = await response.json();
            errorDiv.textContent = data.message || 'Ошибка входа';
            errorDiv.classList.remove('d-none');
            return;
        }

        var data = await response.json();
        localStorage.setItem('token', data.token);
        localStorage.setItem('role', data.role);
        localStorage.setItem('username', data.username);
        if (data.driverId) localStorage.setItem('driverId', data.driverId);

        if (data.role === 'DRIVER') window.location = '/driver/dashboard';
        else if (data.role === 'DISPATCHER') window.location = '/dispatcher/dashboard';
        else if (data.role === 'MEDIC') window.location = '/medical/panel';
        else if (data.role === 'MECHANIC') window.location = '/mechanic/panel';
        else window.location = '/admin/vehicles';
    } catch (e) {
        errorDiv.textContent = 'Ошибка соединения с сервером';
        errorDiv.classList.remove('d-none');
    }
}