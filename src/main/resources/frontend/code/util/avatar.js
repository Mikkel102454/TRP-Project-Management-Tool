function renderAvatars(initialsList) {
    const maxVisible = 3;

    const visible = initialsList.slice(0, maxVisible);
    const remaining = initialsList.length - maxVisible;

    let html = `<div class="flex -space-x-2 mb-2">`;

    visible.forEach((initials, i) => {
        html += `
            <div class="w-7 h-7 bg-blue-200 rounded-full flex items-center justify-center text-xs font-bold">
                ${initials}
            </div>
        `;
    });

    if (remaining > 0) {
        html += `
            <div class="w-7 h-7 bg-gray-200 rounded-full flex items-center justify-center text-xs font-bold">
                +${remaining}
            </div>
        `;
    }

    html += `</div>`;

    return html;
}