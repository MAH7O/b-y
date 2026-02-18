// Gallery logic
async function loadGallery() {
    const response = await fetch(BACKEND_URL + "/images");
    const images = await response.json();
    const container = document.getElementById("gallery-container");
    if (!container) return;
    container.innerHTML = images.map((img: any) => `
        <img src="${BACKEND_URL}/uploads/${img.path}" class="img-fluid m-2" style="width: 200px;">
    `).join("");
}

function getGalleryImageUrl(path: string): string {
    if (path.startsWith('http')) return path;
    const filename = path.split('/').pop();
    return `${BACKEND_URL}/uploads/${filename}`;
}

loadGallery();
