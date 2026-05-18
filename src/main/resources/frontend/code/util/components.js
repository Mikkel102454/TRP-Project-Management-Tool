const loadedComponents = new Map();

async function getComponent(name) {
    if (loadedComponents.has(name)) {
        return loadedComponents.get(name);
    }

    try {
        const response = await fetch(
            `${DOMAIN_ROOT + CONTEXT_PATH}/components/${name}`,
            {
                method: "GET",
            }
        );

        if (!response.ok) {
            log("Failed to load component: " + name, Levels.WARNING);
            return null;
        }

        const data = await response.text();

        loadedComponents.set(name, data);

        return data;
    } catch (e) {
        log(e, Levels.SEVERE);
        return null;
    }
}

function evaluateCondition(condition) {
    const match = condition.match(
        /^(.+?)\s*(==|!=|>=|<=|>|<)\s*(.+)$/
    );

    if (!match) return false;

    let [, left, operator, right] = match;

    left = parseValue(left.trim());
    right = parseValue(right.trim());
    switch (operator) {
        case "==":
            return left == right;

        case "!=":
            return left != right;

        case ">":
            return left > right;

        case "<":
            return left < right;

        case ">=":
            return left >= right;

        case "<=":
            return left <= right;

        default:
            return false;
    }
}

function parseValue(value) {
    // Remove quotes
    if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
    ) {
        return value.slice(1, -1);
    }

    // Boolean
    if (value === "true") return true;
    if (value === "false") return false;

    // Number
    if (!isNaN(value)) return Number(value);

    return value;
}

function processIfBlocks(template, data) {
    const lines = template.split("\n");

    const stack = [];
    let output = "";

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();

        // #if
        if (trimmed.startsWith("#if ")) {
            let condition = trimmed.substring(4).trim();

            Object.keys(data).forEach(key => {
                condition = condition.replaceAll(
                    `{{${key}}}`,
                    JSON.stringify(data[key])
                );
            });

            condition = condition.replace(/\r/g, "").trim();

            const result = evaluateCondition(condition);

            stack.push(result);

            continue;
        }

        // #endif
        if (trimmed === "#endif") {
            stack.pop();
            continue;
        }

        // Render only if ALL parents are true
        const shouldRender =
            stack.length === 0 ||
            stack.every(v => v === true);

        if (shouldRender) {
            output += line + "\n";
        }
    }

    return output;
}

function updateComponent(component, data = {}) {
    if (!component) return "";

    component = processIfBlocks(component, data);

    Object.keys(data).forEach(key => {
        component = component.replaceAll(
            `{{${key}}}`,
            data[key] ?? ""
        );
    });

    return component;
}