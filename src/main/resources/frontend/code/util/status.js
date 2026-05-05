const allStatuses = new Map([
    ["TODO", { label: "Todo", color: "bg-blue-500" }],
    ["AWAITING_CONFIRMATION", { label: "Awaiting confirmation", color: "bg-yellow-500" }],
    ["FINISHED", { label: "Finished", color: "bg-green-500" }]
]);

function getColorFromStatus(status){
    return allStatuses.get(status).color
}

function getDropdownOptions(selectedStatus = null) {
    let html = "";

    for (const [key, value] of allStatuses) {
        const selected = key === selectedStatus ? "selected" : "";
        html += `<option value="${key}" ${selected}>${value.label}</option>`;
    }

    return html;
}