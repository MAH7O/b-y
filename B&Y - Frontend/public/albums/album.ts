// Albums logic
async function loadAlbums() {
    const response = await fetch(BACKEND_URL + "/albums");
    const albums = await response.json();
    const grid = document.getElementById("album-grid");
    if (!grid) return;
    grid.innerHTML = albums.map((album: any) => `
        <div class="col-md-4 mb-4" data-tilt>
            <div class="card bg-dark border-secondary">
                <div class="card-body">
                    <h5 class="card-title">${album.name}</h5>
                    <a href="/albumImage?id=${album.id}" class="btn btn-primary">View</a>
                </div>
            </div>
        </div>
    `).join("");
    // @ts-ignore
    VanillaTilt.init(document.querySelectorAll("[data-tilt]"));
}

loadAlbums();
