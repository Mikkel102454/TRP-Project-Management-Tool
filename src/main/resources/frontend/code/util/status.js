const allStatuses = new Map([
    ["TODO", { label: "Todo", color: "bg-blue-500" }],
    ["AWAITING_CONFIRMATION", { label: "Awaiting confirmation", color: "bg-yellow-500" }],
    ["FINISHED", { label: "Finished", color: "bg-green-500" }],
    ["CLOSED", { label: "Closed", color: "bg-gray-500" }]
]);

function getColorFromStatus(status){
    return allStatuses.get(status).color
}

async function getDropdownOptions(selectedStatus = null) {
    let html = "";
    let user = await getUser();
    for (const [key, value] of allStatuses) {
        if(key === "CLOSED" && !user.isAdmin) continue;
        const selected = key === selectedStatus ? "selected" : "";
        html += `<option value="${key}" ${selected}>${value.label}</option>`;
    }

    return html;
}