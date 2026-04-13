const Levels = Object.freeze({
    INFO: 0,
    WARNING: 1,
    SEVERE: 2
});

function log(msg, level){
    switch (level) {
        case 0:
            console.log(msg);
            break;
        case 1:
            console.warn(msg);
            break;
        case 2:
            console.error(msg);
            break;
        default:
            console.warn("---------------------------------");
            console.warn("   No valid log level selected   ");
            console.warn("---------------------------------");
            console.log(msg);
            break;
    }
}