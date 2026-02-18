// Admin management logic
async function fetchUsers() {
    try {
        const response = await fetch(BACKEND_URL + "/users");
        if (response.ok) {
            const users = await response.json();
            renderUsers(users);
        }
    } catch (error) {
        console.error("Failed to fetch users", error);
    }
}

function renderUsers(users: any[]) {
    const tableBody = document.getElementById("user-table-body");
    if (!tableBody) return;
    tableBody.innerHTML = users.map(user => `
        <tr>
            <td>${user.username}</td>
            <td><span class="badge bg-info">${user.role}</span></td>
            <td>
                <button class="btn btn-sm btn-warning">Edit</button>
                <button class="btn btn-sm btn-danger">Delete</button>
            </td>
        </tr>
    `).join("");
}

document.addEventListener("DOMContentLoaded", fetchUsers);
