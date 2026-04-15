function timeShorter(time) {
    const units = [
        { label: 'Y', value: 60 * 60 * 24 * 365 },
        { label: 'MO', value: 60 * 60 * 24 * 30 },
        { label: 'D', value: 60 * 60 * 24 },
        { label: 'H', value: 60 * 60 },
        { label: 'M', value: 60 },
        { label: 'S', value: 1 }
    ];

    for (let unit of units) {
        if (time >= unit.value) {
            const result = Math.floor(time / unit.value);
            return result + unit.label;
        }
    }

    return "0s";
}

function timeShorterLong(time) {
    const units = [
        { label: ' years', value: 60 * 60 * 24 * 365 },
        { label: ' months', value: 60 * 60 * 24 * 30 },
        { label: ' days', value: 60 * 60 * 24 },
        { label: ' hours', value: 60 * 60 },
        { label: ' minutes', value: 60 },
        { label: ' seconds', value: 1 }
    ];

    for (let unit of units) {
        if (time >= unit.value) {
            const result = Math.floor(time / unit.value);
            return result + unit.label;
        }
    }

    return "0 seconds";
}

function timeLongerShort(input) {
    const units = {
        Y: 60 * 60 * 24 * 365,
        MO: 60 * 60 * 24 * 30,
        D: 60 * 60 * 24,
        H: 60 * 60,
        M: 60,
        S: 1
    };

    const match = input.match(/^(\d+)(Y|MO|D|H|M|S)$/i);
    if (!match) return 0;

    const value = parseInt(match[1], 10);
    const unit = match[2].toUpperCase();

    return value * (units[unit] || 0);
}

function timeLongerLong(input) {
    const units = {
        years: 60 * 60 * 24 * 365,
        year: 60 * 60 * 24 * 365,
        months: 60 * 60 * 24 * 30,
        month: 60 * 60 * 24 * 30,
        days: 60 * 60 * 24,
        day: 60 * 60 * 24,
        hours: 60 * 60,
        hour: 60 * 60,
        minutes: 60,
        minute: 60,
        seconds: 1,
        second: 1
    };

    const match = input.match(/^(\d+)\s*(\w+)$/i);
    if (!match) return 0;

    const value = parseInt(match[1], 10);
    const unit = match[2].toLowerCase();

    return value * (units[unit] || 0);
}