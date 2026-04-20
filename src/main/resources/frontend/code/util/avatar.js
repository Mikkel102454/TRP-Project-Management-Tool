function renderAvatars(initialsList) {
    const maxVisible = 3;

    const visible = initialsList.slice(0, maxVisible);
    const remaining = initialsList.length - maxVisible;

    let html = ``;

    visible.forEach((initials) => {
        html += `
            <div class="w-8 h-8 bg-blue-200 rounded-full flex items-center justify-center text-xs font-bold leading-none border border-white">
                ${initials}
            </div>
        `;
    });

    if (remaining > 0) {
        html += `
            <div class="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center text-xs font-bold leading-none border border-white">
                +${remaining}
            </div>
        `;
    }

    return html;
}