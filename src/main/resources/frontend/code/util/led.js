const greenLed =
    '<div class="flex justify-end">\n' +
    '    <div class="relative w-6 h-6">\n' +
    '        <div class="absolute inset-0 bg-green-500 rounded-full blur-sm opacity-50 -m-1"></div>\n' +
    '        <div class="absolute inset-0 bg-green-500 rounded-full border-2 border-green-700"></div>\n' +
    '        <div class="absolute top-1 left-1 w-3 h-3 bg-green-200 rounded-full opacity-70 blur-sm"></div>\n' +
    '        <div class="absolute inset-1 bg-green-300 rounded-full opacity-40"></div>\n' +
    '    </div>\n' +
    '</div>'
const offLed =
    '<div class="flex justify-end">\n' +
    '    <div class="relative w-6 h-6">\n' +
    '        <div class="absolute inset-0 bg-gray-500 rounded-full border-2 border-gray-400"></div>\n' +
    '        <div class="absolute top-1 left-1 w-3 h-3 bg-gray-300 rounded-full opacity-40 blur-sm"></div>\n' +
    '    </div>\n' +
    '</div>'
const yellowLed =
    '<div class="flex justify-end">\n' +
    '    <div class="relative w-6 h-6">\n' +
    '        <div class="absolute inset-0 bg-yellow-400 rounded-full blur-sm opacity-50 -m-1"></div>\n' +
    '        <div class="absolute inset-0 bg-yellow-500 rounded-full border-2 border-yellow-600"></div>\n' +
    '        <div class="absolute top-1 left-1 w-3 h-3 bg-yellow-200 rounded-full opacity-70 blur-sm"></div>\n' +
    '        <div class="absolute inset-1 bg-yellow-300 rounded-full opacity-40"></div>\n' +
    '    </div>\n' +
    '</div>'

function renderLed(type){
    switch (type){
        case "green":
            return greenLed
        case "off":
            return offLed
        case "yellow":
            return yellowLed
    }
}