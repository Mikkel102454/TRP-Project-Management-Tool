const loadedComponents = new Map();

async function getComponent(name){
    if(loadedComponents.has(name)){
        return loadedComponents.get(name)
    }

    try {
        const response = await fetch(`${DOMAIN_ROOT + CONTEXT_PATH}/components/` + name, {
            method: "GET",
        });

        if (!response.ok) {
            log("Failed to load component: " + name, Levels.WARNING)
            return null;
        }

        const data = await response.text();

        loadedComponents.set(name, data)

        return data;
    } catch (e) {
        log(e, Levels.SEVERE);
        return null;
    }
}

function updateComponent(component, data = {}){
    if (!component) return "";

    Object.keys(data).forEach(key => {
        component = component.replaceAll(`{{${key}}}`, data[key]);
    });

    return component;
}