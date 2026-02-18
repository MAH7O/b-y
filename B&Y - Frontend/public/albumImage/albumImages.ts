// Logic for album images
const albumId = new URLSearchParams(window.location.search).get("id");

async function loadAlbumImages() {
    if (!albumId) return;
    const response = await fetch(BACKEND_URL + `/albums/${albumId}/images`);
    const images = await response.json();
    const grid = document.getElementById("image-grid");
    if (!grid) return;
    grid.innerHTML = images.map((img: any) => `
        <div class="col-md-3 mb-4">
            <div class="card bg-dark border-secondary">
                <img src="${BACKEND_URL}/uploads/${img.path}" class="card-img-top">
                <div class="card-body">
                    <h5 class="card-title">${img.title}</h5>
                </div>
            </div>
        </div>
    `).join("");
}

loadAlbumImages();
